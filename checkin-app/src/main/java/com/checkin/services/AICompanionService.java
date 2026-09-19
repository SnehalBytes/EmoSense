package com.checkin.services;

import com.checkin.config.AIConfiguration;
import com.checkin.model.AnalysisResult;
import com.checkin.model.ChatMessage;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Service orchestrating the AI Companion conversation session for EmoSense.
 *
 * Target Architecture:
 * - Directs traffic between GeminiCompanionEngine (external API) and PatternBasedCompanionEngine (local fallback).
 * - Safety checks (crisis, self-harm, medical diagnosis inquiries) run BEFORE calling any external API.
 * - Graceful fallback on API failure, timeout, 4xx/5xx, missing key, or network unavailability.
 * - Preserves session conversation history and recent check-in context without leaking sensitive data.
 */
public class AICompanionService {

    public enum EngineMode {
        ONLINE,
        OFFLINE_FALLBACK
    }

    private final List<ChatMessage> conversationHistory = new ArrayList<>();
    private AnalysisResult latestContext;

    private final CompanionEngine primaryEngine;
    private final CompanionEngine fallbackEngine;

    private EngineMode currentMode = EngineMode.OFFLINE_FALLBACK;
    private boolean lastResponseUsedFallback = false;

    public AICompanionService() {
        this.fallbackEngine = new PatternBasedCompanionEngine();
        if (AIConfiguration.isApiKeyAvailable()) {
            CompanionEngine online = null;
            try {
                online = new GeminiCompanionEngine();
                this.currentMode = EngineMode.ONLINE;
            } catch (Exception e) {
                online = null;
                this.currentMode = EngineMode.OFFLINE_FALLBACK;
            }
            this.primaryEngine = online;
        } else {
            this.primaryEngine = null;
            this.currentMode = EngineMode.OFFLINE_FALLBACK;
        }
    }

    public AICompanionService(CompanionEngine engine) {
        this(engine, new PatternBasedCompanionEngine());
    }

    public AICompanionService(CompanionEngine primaryEngine, CompanionEngine fallbackEngine) {
        this.primaryEngine = primaryEngine;
        this.fallbackEngine = fallbackEngine != null ? fallbackEngine : new PatternBasedCompanionEngine();
        this.currentMode = (this.primaryEngine != null) ? EngineMode.ONLINE : EngineMode.OFFLINE_FALLBACK;
    }

    public CompanionEngine getPrimaryEngine() {
        return primaryEngine;
    }

    public CompanionEngine getFallbackEngine() {
        return fallbackEngine;
    }

    public EngineMode getCurrentMode() {
        return currentMode;
    }

    public boolean isLastResponseUsedFallback() {
        return lastResponseUsedFallback;
    }

    public boolean isOnlineEngineConfigured() {
        return primaryEngine != null;
    }

    public void setCheckInContext(AnalysisResult result) {
        this.latestContext = result;
    }

    public AnalysisResult getCheckInContext() {
        return latestContext;
    }

    public boolean hasCheckInContext() {
        return latestContext != null && (latestContext.hasFacial() || latestContext.hasText() || latestContext.getCombinedLabel() != null);
    }

    /**
     * Formats a human-readable, non-clinical summary of the latest check-in.
     * Does not invent missing values or expose raw tensor internals.
     */
    public String getFormattedContextSummary() {
        if (!hasCheckInContext()) {
            return null;
        }

        StringBuilder sb = new StringBuilder("Latest EmoSense check-in:\n");
        if (latestContext.hasFacial() && latestContext.getFacialLabel() != null) {
            sb.append("• Facial signal: ").append(latestContext.getFacialLabel());
            if (latestContext.getFacialConfidence() > 0) {
                sb.append(String.format(" (%.0f%% confidence)", latestContext.getFacialConfidence()));
            }
            sb.append("\n");
        }
        if (latestContext.hasText() && latestContext.getTextLabel() != null) {
            sb.append("• Textual cue: ").append(latestContext.getTextLabel());
            if (latestContext.getTextConfidence() > 0) {
                sb.append(String.format(" (%.0f%% confidence)", latestContext.getTextConfidence()));
            }
            sb.append("\n");
        }
        if (latestContext.getCombinedLabel() != null && !latestContext.getCombinedLabel().isBlank()) {
            sb.append("• Multimodal insight: ").append(latestContext.getCombinedLabel());
            if (latestContext.getCombinedConfidence() > 0) {
                sb.append(String.format(" (%.0f%% confidence)", latestContext.getCombinedConfidence()));
            }
            sb.append("\n");
        }
        return sb.toString().trim();
    }

    /**
     * Adds a user message to conversation memory without triggering an immediate response.
     */
    public ChatMessage addUserMessage(String userText) {
        if (userText == null || userText.trim().isEmpty()) {
            return null;
        }
        ChatMessage userMsg = ChatMessage.user(userText.trim());
        conversationHistory.add(userMsg);
        return userMsg;
    }

    /**
     * Computes the companion reply text based on conversation history and safety guardrails,
     * without modifying the conversation history.
     */
    public String computeCompanionReplyText() {
        if (conversationHistory.isEmpty()) {
            return null;
        }
        ChatMessage lastUserMsg = null;
        for (int i = conversationHistory.size() - 1; i >= 0; i--) {
            if (conversationHistory.get(i).isUser()) {
                lastUserMsg = conversationHistory.get(i);
                break;
            }
        }
        String prompt = lastUserMsg != null ? lastUserMsg.text() : "";
        String replyText;

        // 1. PRE-FLIGHT SAFETY & CRISIS GUARDRAIL
        // Checks happen BEFORE sending any message to external API
        if (isCrisisMessage(prompt)) {
            replyText = getCrisisResponse();
            lastResponseUsedFallback = false;
        } else if (isMedicalDiagnosisInquiry(prompt)) {
            replyText = getMedicalDisclaimerResponse(latestContext);
            lastResponseUsedFallback = false;
        } else {
            // 2. ATTEMPT PRIMARY ENGINE (e.g. Gemini)
            String primaryReply = null;
            if (primaryEngine != null) {
                try {
                    primaryReply = primaryEngine.generateReply(prompt, Collections.unmodifiableList(conversationHistory), latestContext);
                } catch (Exception ex) {
                    // Silently fall back; never expose technical errors or API keys
                    primaryReply = null;
                }
            }

            // 3. FALLBACK TO LOCAL PATTERN ENGINE IF NEEDED
            if (primaryReply != null && !primaryReply.isBlank()) {
                replyText = primaryReply.trim();
                currentMode = EngineMode.ONLINE;
                lastResponseUsedFallback = false;
            } else {
                currentMode = EngineMode.OFFLINE_FALLBACK;
                lastResponseUsedFallback = true;
                try {
                    replyText = fallbackEngine.generateReply(prompt, Collections.unmodifiableList(conversationHistory), latestContext);
                } catch (Exception ex) {
                    replyText = "I hear you. That sounds like something worth giving yourself a little space to reflect on. If you'd like, you can tell me more about what happened.";
                }
            }
        }

        return replyText;
    }

    /**
     * Appends a companion message to conversation history.
     */
    public ChatMessage addCompanionMessage(String replyText) {
        if (replyText == null || replyText.trim().isEmpty()) {
            replyText = "I hear you. Take a moment for yourself, and feel free to share whatever is on your mind whenever you are ready.";
        }
        ChatMessage companionMsg = ChatMessage.companion(replyText.trim());
        conversationHistory.add(companionMsg);
        return companionMsg;
    }

    /**
     * Generates and appends a companion response based on the current conversation history.
     * Evaluates safety guardrails BEFORE delegating to external API.
     * Automatically falls back to PatternBasedCompanionEngine on any API issue.
     */
    public ChatMessage generateCompanionResponse() {
        String replyText = computeCompanionReplyText();
        if (replyText == null) {
            return null;
        }
        return addCompanionMessage(replyText);
    }

    /**
     * Asynchronously generates a companion response on a background worker thread.
     */
    public CompletableFuture<ChatMessage> generateCompanionResponseAsync() {
        return CompletableFuture.supplyAsync(this::generateCompanionResponse);
    }

    /**
     * Sends a user message, processes it through the response engine,
     * updates session memory, and returns the companion's reply synchronously.
     */
    public ChatMessage sendMessage(String userText) {
        ChatMessage u = addUserMessage(userText);
        if (u == null) return null;
        return generateCompanionResponse();
    }

    public List<ChatMessage> getConversationHistory() {
        return Collections.unmodifiableList(conversationHistory);
    }

    /**
     * Resets conversation memory for the active companion session.
     * Strictly does not alter database tables, check-in history, or user data.
     */
    public void clearConversation() {
        conversationHistory.clear();
    }

    // =========================================================================
    // SAFETY & CRISIS GUARDRAILS (Evaluated before any external API request)
    // =========================================================================

    public static boolean isCrisisMessage(String prompt) {
        if (prompt == null) return false;
        String lower = prompt.toLowerCase().trim();
        return containsAny(lower,
                "suicide", "kill myself", "killing myself", "end my life", "ending my life",
                "harm myself", "harming myself", "hurt myself", "hurting myself",
                "don't want to live", "dont want to live", "want to die", "wishing to die",
                "better off dead"
        );
    }

    public static String getCrisisResponse() {
        return "I hear that you're going through a deeply overwhelming and painful moment. Because your safety matters, please consider connecting with someone who can support you right now. You can talk with a trusted friend, family member, or reach out to a support service such as a local crisis line (dial or text 988 in the US/Canada, or contact your local emergency services). You don't have to carry this alone, and supportive people are available to help.";
    }

    public static boolean isMedicalDiagnosisInquiry(String prompt) {
        if (prompt == null) return false;
        String lower = prompt.toLowerCase().trim();
        return containsAny(lower,
                "diagnose", "diagnosis", "clinical depression", "anxiety disorder",
                "bipolar", "mentally ill", "mental illness", "psychiatry", "psychiatric",
                "medical opinion", "medical advice"
        ) || (containsAny(lower, "do i have", "am i suffering", "am i diagnosed") &&
                containsAny(lower, "depression", "anxiety", "disorder", "illness", "condition"));
    }

    public static String getMedicalDisclaimerResponse(AnalysisResult context) {
        String signalPart = "";
        if (context != null && context.getCombinedLabel() != null) {
            signalPart = "Your recent check-in shows a " + context.getCombinedLabel().toLowerCase() + " emotional signal, but this is an observational indicator rather than a medical assessment. ";
        } else if (context != null && context.hasFacial()) {
            signalPart = "Your recent check-in shows a " + context.getFacialLabel().toLowerCase() + "-related emotional signal, but this is an observational indicator rather than a medical assessment. ";
        }
        return "EmoSense provides emotional reflections, not medical or psychological diagnosis. " + signalPart +
                "If you are experiencing persistent emotional distress or have questions about mental health conditions, we strongly encourage speaking with a qualified healthcare or mental health professional.";
    }

    private static boolean containsAny(String input, String... candidates) {
        for (String candidate : candidates) {
            if (input.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Modular interface allowing integration of external APIs or fallback logic.
     */
    public interface CompanionEngine {
        String generateReply(String userMessage, List<ChatMessage> history, AnalysisResult context) throws Exception;
    }

    /**
     * Production-ready Java-based supportive reflection engine adhering to EmoSense guidelines:
     * - Empathetic and conversational
     * - Non-judgmental and supportive
     * - Strict medical/psychiatric diagnosis prevention
     * - Safety crisis resources for self-harm keywords
     * - Session context and multi-turn memory
     */
    /**
     * Production-ready Java-based supportive reflection engine adhering to EmoSense guidelines:
     * - Contextual intent and emotion cue detection across 10 distinct conversational categories
     * - Multi-template deterministic variation (stable in tests, no random glitches)
     * - No repetitive clichés ("I'm sorry you're feeling this way...", "Take a deep breath...", "You're not alone")
     * - Empathetic, conversational, and practical suggestions for academic/work situations
     * - Strict medical/psychiatric diagnosis prevention
     * - Safety crisis resources for self-harm keywords
     * - Natural integration of recent check-in context using uncertainty-aware phrasing
     */
    public static class PatternBasedCompanionEngine implements CompanionEngine {

        private static int pickIndex(String text, int modulo) {
            if (modulo <= 1) return 0;
            return (Math.abs(text.trim().toLowerCase().hashCode()) ^ (text.length() * 31)) % modulo;
        }

        @Override
        public String generateReply(String userMessage, List<ChatMessage> history, AnalysisResult context) {
            if (userMessage == null || userMessage.isBlank()) {
                return "I'm here whenever you'd like to share or reflect on what's on your mind.";
            }
            String lower = userMessage.toLowerCase().trim();

            // 1. SAFETY & CRISIS GUARDRAIL
            if (isCrisisMessage(lower)) {
                return getCrisisResponse();
            }

            // 2. MEDICAL / PSYCHIATRIC DIAGNOSIS GUARDRAIL
            if (isMedicalDiagnosisInquiry(lower)) {
                return getMedicalDisclaimerResponse(context);
            }

            // 3. CHECK-IN QUERY
            if (containsAny(lower, "my check-in", "what did my check-in say", "my check in", "check-in results", "check in results", "what was my emotion", "what did my results say")) {
                if (context != null && (context.hasFacial() || context.hasText() || context.getCombinedLabel() != null)) {
                    StringBuilder desc = new StringBuilder("Based on your latest check-in reflection: ");
                    if (context.getCombinedLabel() != null && !context.getCombinedLabel().isBlank()) {
                        desc.append("the multimodal signal pattern suggested a ").append(context.getCombinedLabel().toLowerCase()).append(" cue. ");
                    } else if (context.hasFacial()) {
                        desc.append("the facial cue observed suggested a ").append(context.getFacialLabel().toLowerCase()).append(" signal. ");
                    }
                    if (context.hasText() && context.getTextLabel() != null) {
                        desc.append("Your textual expression pointed toward ").append(context.getTextLabel().toLowerCase()).append(". ");
                    }
                    desc.append("Keep in mind these are observational signals rather than definitive conclusions. How does that reflection compare to what you're feeling right now?");
                    return desc.toString();
                } else {
                    return "You haven't submitted a check-in in this session yet. You can start a check-in anytime from the Check-In screen, or share whatever is on your mind right now.";
                }
            }

            // 4. MULTI-TURN CONVERSATION MEMORY: Contextual follow-ups
            if (history != null && history.size() >= 2) {
                String previousUser = null;
                String previousAssistant = null;

                for (int i = history.size() - 2; i >= 0; i--) {
                    ChatMessage msg = history.get(i);
                    if (previousAssistant == null && msg.isCompanion()) {
                        previousAssistant = msg.text().toLowerCase();
                    } else if (previousUser == null && msg.isUser()) {
                        previousUser = msg.text().toLowerCase();
                    }
                    if (previousAssistant != null && previousUser != null) break;
                }

                // Follow-up to competition / sports / race / achievement:
                boolean prevWasCompetition = (previousUser != null && containsAny(previousUser, "competition", "race", "running", "won", "tournament", "contest", "match", "game")) ||
                        (previousAssistant != null && containsAny(previousAssistant, "competition", "race", "running", "winning", "achievement"));

                if (prevWasCompetition) {
                    if (containsAny(lower, "nervous", "scared", "anxious", "doubt", "fear", "worried", "before it started", "before the competition", "before the race", "heart was racing", "pressure")) {
                        return "It's completely natural to feel nervous before a big competition—it shows how much you cared about doing your best! Pushing past those nerves makes your win even more meaningful. How did that nervousness change once the race actually started?";
                    }
                    if (containsAny(lower, "proud", "happy", "relieved", "excited", "celebrated", "celebrating", "good", "great", "amazing")) {
                        return "That sense of pride and relief is so well-deserved. Celebrate this win and remember what you're capable of when you dedicate yourself to a goal!";
                    }
                    if (containsAny(lower, "hard", "difficult", "tired", "exhausting", "close", "tough")) {
                        return "Competitions push you to your physical and mental limits, which makes crossing that finish line and winning all the more rewarding. Are you giving yourself some time to rest and recover today?";
                    }
                }

                if (previousAssistant != null && (previousAssistant.contains("what made it difficult") || previousAssistant.contains("tell me more about what happened") || previousAssistant.contains("weighing on you"))) {
                    if (containsAny(lower, "college", "work", "studies", "classes", "university", "assignment", "exam", "school", "academics")) {
                        return "Carrying heavy academic expectations while managing daily tasks can quickly drain your energy. What part of that felt like the steepest climb today?";
                    }
                    if (containsAny(lower, "friends", "friend", "argument", "relationship", "partner", "people", "fight", "broke up")) {
                        return "Navigating interpersonal tension or misunderstandings can linger in your mind long after the interaction. Would you like to talk through what happened?";
                    }
                }
            }

            // 5. GRATITUDE (Category G)
            if (containsAny(lower, "thank you", "thanks", "really needed that", "appreciate it", "grateful", "very helpful")) {
                String[] gratitudeTemplates = {
                    "You're very welcome! Taking time to pause and reflect on your thoughts is a valuable habit. I'm here whenever you want to check in again.",
                    "Glad this conversation felt helpful. Be kind to yourself today, and feel free to return whenever you need a grounded sounding board.",
                    "You're welcome! Honoring where you are mentally and giving yourself space to talk it through makes a big difference. Wishing you a calm rest of your day."
                };
                return gratitudeTemplates[pickIndex(userMessage, gratitudeTemplates.length)];
            }

            // 6. CELEBRATION / EXCITEMENT / ACHIEVEMENT (Category F)
            boolean isCelebration = containsAny(lower,
                    "finally finished", "finished my", "completed my", "finally completed",
                    "passed my", "passed the", "got an a", "won", "winner", "achievement",
                    "achieved", "accomplished", "milestone", "great news", "excited about",
                    "so happy that", "proud of", "did it", "nailed it", "crushed it", "first place"
            );
            if (isCelebration) {
                if (containsAny(lower, "running competition", "running", "race", "competition", "tournament", "contest", "marathon", "track")) {
                    String[] sportsTemplates = {
                        "That's wonderful! Winning a running competition is something to be proud of. You put in the effort and it paid off. How are you feeling about the achievement?",
                        "Congratulations on that victory! Crossing the finish line first in a competition takes real dedication and perseverance. Celebrate this win and be proud of yourself!",
                        "What an incredible achievement! Stepping up to compete and coming out on top is a huge milestone. You should feel wonderful and proud of what you accomplished today!"
                    };
                    return sportsTemplates[pickIndex(userMessage, sportsTemplates.length)];
                }

                if (containsAny(lower, "project", "assignment", "paper", "thesis", "code", "coding")) {
                    String[] projectTemplates = {
                        "Congratulations on crossing the finish line! Finishing a project requires real persistence, especially when dealing with hurdles along the way. What part of the completed work are you most satisfied with?",
                        "That is fantastic news! Seeing all that effort culminate in a finished project is an awesome feeling. Make sure you give yourself credit and take some time to enjoy the milestone.",
                        "Huge milestone! Wrapping up a project lifts a major weight off your mind. What was the toughest part you managed to solve along the way?"
                    };
                    return projectTemplates[pickIndex(userMessage, projectTemplates.length)];
                }

                String[] generalCelebrationTemplates = {
                    "That is wonderful news! Savoring achievements and letting yourself feel genuine pride is so important. How does it feel to see your hard work pay off?",
                    "Congratulations! Reaching that milestone shows how much dedication you invested. That sense of pride is so well-deserved!",
                    "What a wonderful achievement! Pausing to celebrate your progress reinforces all the effort you put in. You should be proud of yourself today!"
                };
                return generalCelebrationTemplates[pickIndex(userMessage, generalCelebrationTemplates.length)];
            }

            // 7. SADNESS / DISAPPOINTMENT / EFFORT-OUTCOME MISMATCH (Category B)
            boolean isDisappointment = containsAny(lower,
                    "disappointed", "disappointing", "result was bad", "result was disappointing",
                    "worked so hard but", "studied so much but", "failed", "didn't pass", "didnt pass",
                    "poor grade", "bad grade", "let down", "upset with myself", "discouraged", "hurt by"
            );
            if (isDisappointment) {
                String[] disappointmentTemplates = {
                    "It is deeply discouraging to invest sincere time and effort and have the outcome fall short of what you hoped for. That disappointment makes total sense. Give yourself a little room to process that sting before jumping into problem-solving.",
                    "Putting real dedication into something only to face a disappointing result hurts, and there's no need to rush into finding a silver lining right away. What felt most unfair or unexpected about the outcome?",
                    "That kind of mismatch between your effort and the final result is genuinely frustrating. Be gentle with your expectations today—one result doesn't erase the work and discipline you demonstrated. What feels like the hardest part to sit with right now?"
                };
                return disappointmentTemplates[pickIndex(userMessage, disappointmentTemplates.length)];
            }

            // 8. ANXIETY / WORRY / EXAM & DEADLINE APPREHENSION (Category D)
            boolean isWorry = containsAny(lower,
                    "worried", "scared", "afraid", "nervous", "concerned", "dread",
                    "exam tomorrow", "tomorrow's exam", "scared about", "freaking out",
                    "anxious about", "terrified", "fear of"
            );
            if (isWorry) {
                if (containsAny(lower, "exam", "test", "finals", "midterm", "quiz", "tomorrow")) {
                    String[] examWorryTemplates = {
                        "That sounds like a stressful situation, especially with an exam right around the corner. When anticipatory worry peaks, trying to cram everything late will only heighten tension. Picking one key summary area to review for 20 minutes and getting good rest will serve your clarity far better.",
                        "Anticipating tomorrow's exam can easily make your mind overlook what you already know. Trust that the preparation you've done is present. What is one specific topic that would give you the most peace of mind if you gave it a brief, focused look right now?",
                        "Facing a big exam tomorrow brings a lot of natural tension. Rather than running through every worst-case scenario, bring your focus to the immediate next hour: review your core notes, hydrate, and give your mind permission to unwind before bed."
                    };
                    return examWorryTemplates[pickIndex(userMessage, examWorryTemplates.length)];
                }

                String[] generalWorryTemplates = {
                    "That sounds like a tense situation to be carrying. When uncertainty kicks in, it's easy for your thoughts to run ahead of reality. What is one small factor in this situation that is still within your direct control?",
                    "Uncertainty can be genuinely unsettling. Instead of trying to resolve all the unknowns at once, what is the most immediate concern that feels heaviest to you right now?",
                    "It sounds like there are a lot of what-ifs swirling in your thoughts. Grounding yourself in the present moment can help steady things. What is one tangible next step you can take today?"
                };
                return generalWorryTemplates[pickIndex(userMessage, generalWorryTemplates.length)];
            }

            // 9. FRUSTRATION / ANGER / OBSTACLES (Category C)
            boolean isFrustration = containsAny(lower,
                    "frustrated", "frustrating", "angry", "annoyed", "irritated", "fed up",
                    "going wrong", "went wrong", "everything went wrong", "nothing is working",
                    "hate this", "mad at", "pissed", "furious", "ruined my day"
            );
            if (isFrustration) {
                String[] frustrationTemplates = {
                    "When one thing after another derails, the compounding frustration can feel unbearable. It's completely valid to feel fed up with how today unfolded. What is one thing right now that you can step away from or control?",
                    "Days where every plan seems to run into a wall are thoroughly exhausting. Rather than fighting every obstacle simultaneously, let's call a short timeout: what felt like the main trigger where things began sliding off track?",
                    "That level of irritation makes complete sense when circumstances refuse to cooperate. Taking an intentional pause can help protect your energy. Is there anything from today's plans that can be safely set aside until tomorrow?"
                };
                return frustrationTemplates[pickIndex(userMessage, frustrationTemplates.length)];
            }

            // 10. CONFUSION / LACK OF DIRECTION (Category H)
            boolean isConfusion = containsAny(lower,
                    "don't know what i should do", "dont know what i should do", "don't know what to do",
                    "dont know what to do", "what should i do next", "what do i do next", "don't know",
                    "dont know", "unsure what", "confused", "stuck", "at a loss", "no idea where to start",
                    "which way to go", "indecisive"
            );
            if (isConfusion) {
                String[] confusionTemplates = {
                    "When you're unsure of what step to take next, trying to solve the entire path at once usually creates more mental clutter. What is the single smallest, most immediate decision you can make in the next half hour?",
                    "Feeling stuck at a crossroads is disorienting, but you don't need all the answers mapped out right this second. What are the two main options currently pulling at your attention?",
                    "It's normal to hit moments where your direction feels fuzzy. Let's simplify: if you could only accomplish one low-pressure task before the day ends, which one would leave you feeling slightly more grounded?"
                };
                return confusionTemplates[pickIndex(userMessage, confusionTemplates.length)];
            }

            // 11. LONELINESS / ISOLATION (Category E)
            boolean isLoneliness = containsAny(lower,
                    "lonely", "alone", "nobody understands", "no one understands", "isolated",
                    "nobody cares", "no friends", "left out", "nobody to talk to", "feeling abandoned"
            );
            if (isLoneliness) {
                String[] lonelinessTemplates = {
                    "Feeling like nobody understands what you're navigating is a heavy and isolating experience. Even when the people around you don't seem to tune in, your experiences and feelings matter. Is there a trusted friend, family member, or mentor you might feel comfortable sending a simple greeting to today?",
                    "That sense of disconnection can be painful to carry in silence. While reflecting here can help sort through your thoughts, genuine human connection is irreplaceable—is there someone in your life who has offered a listening ear in the past?",
                    "Carrying everything on your own when it feels like no one gets it can make any challenge feel twice as heavy. What is something you wish the people in your life could see about what you're going through right now?"
                };
                return lonelinessTemplates[pickIndex(userMessage, lonelinessTemplates.length)];
            }

            // 12. STRESS / WORKLOAD / OVERWHELMED (Category A) & ACADEMIC PRESSURE (Category I)
            boolean isStressOrAcademic = containsAny(lower,
                    "overwhelmed", "too much", "under pressure", "stressed", "stress",
                    "workload", "burnout", "too many things", "exhausted", "deadline",
                    "deadlines", "college work", "college", "university", "assignment",
                    "assignments", "studying all day", "unprepared", "classwork", "homework"
            );
            if (isStressOrAcademic) {
                String[] stressTemplates = {
                    "Carrying that much workload and pressure can feel overwhelming when every task demands your immediate focus. Instead of trying to tackle the entire stack, what is one single item that would give you the most relief if completed first?",
                    "Academic and work pressures accumulate fast, and when that overwhelming exhaustion sets in, even routine tasks feel like climbing a mountain. If you've been grinding for hours, a ten-minute break can restore some breathing room. What's the most urgent deliverable on your plate?",
                    "That sounds like a heavy volume of pressure to balance right now. When everything feels urgent and overwhelming, picking one priority and parking the rest for a few hours is usually the most effective approach. What part of the work feels most manageable to start with?"
                };
                return stressTemplates[pickIndex(userMessage, stressTemplates.length)];
            }

            // 13. FATIGUE / EXHAUSTION
            boolean isFatigue = containsAny(lower, "tired", "exhausted", "drained", "fatigue", "exhaustion", "can't sleep", "burnout");
            if (isFatigue && !containsAny(lower, "exam", "deadline", "project", "assignment", "result was disappointing", "result was bad")) {
                String[] fatigueTemplates = {
                    "It sounds like you're carrying a lot of fatigue right now. That kind of deep exhaustion is very real. Have you had an opportunity to get some restful downtime today?",
                    "Carrying that level of exhaustion can leave you feeling completely drained. Giving yourself permission to rest without guilt is essential. What feels like the most restorative thing you can do right now?",
                    "When fatigue builds up, pushing through often backfires. Honoring your need for rest and taking things off your plate for the evening can help restore your energy. What can be set aside until tomorrow?"
                };
                return fatigueTemplates[pickIndex(userMessage, fatigueTemplates.length)];
            }

            // 14. RELATIONSHIP / SOCIAL CONFLICT
            boolean isRelationship = containsAny(lower,
                    "friend", "friends", "boyfriend", "girlfriend", "partner",
                    "relationship", "broke up", "breakup", "ignored me", "argument",
                    "fight with", "fighting with", "conflict", "talked behind my back",
                    "misunderstanding"
            );
            if (isRelationship) {
                String[] relationshipTemplates = {
                    "Navigating relationship tensions or social misunderstandings can take a heavy emotional toll. It's completely valid to feel hurt or unsettled by this. Would you like to share a little more about what happened?",
                    "Interpersonal conflict can linger in your mind and leave you feeling drained. When things feel strained with someone, taking a step back before responding can help. What feels like the most hurtful part of the interaction?",
                    "Social friction and feeling ignored by people you care about is genuinely painful. You deserve space to be heard. What would help you feel a bit more grounded after this argument?"
                };
                return relationshipTemplates[pickIndex(userMessage, relationshipTemplates.length)];
            }

            // 15. GENERAL CONVERSATION & GREETINGS (Category J)
            if (containsAny(lower, "hello", "hi", "hey", "good morning", "good evening", "good afternoon", "how are you")) {
                if (context != null && context.getCombinedLabel() != null && !context.getCombinedLabel().isBlank()) {
                    return "Hello! I'm here to offer a calm space for reflection. Your recent check-in showed signals suggesting a " +
                            context.getCombinedLabel().toLowerCase() + " pattern. How are things feeling for you right now?";
                }
                String[] greetingTemplates = {
                    "Hello! Welcome back to EmoSense. I'm here to provide a calm, supportive space for whatever is on your mind today. How are you feeling right now?",
                    "Hi there! I'm ready whenever you'd like to pause, reflect, or unpack your thoughts. How has your day been going so far?",
                    "Hey! Good to connect with you. Whether you want to talk through a challenge, celebrate a win, or simply reflect, I'm listening. What's on your mind today?"
                };
                return greetingTemplates[pickIndex(userMessage, greetingTemplates.length)];
            }

            // 16. GENERAL MOOD / SADNESS / DIFFICULT DAY (Category B fallback)
            if (containsAny(lower, "difficult day", "hard day", "tough day", "bad day", "awful day", "rough day", "sad", "down", "unhappy", "crying", "gloomy", "feeling low")) {
                String[] lowMoodTemplates = {
                    "It sounds like today was genuinely difficult and heavy. Honoring that low energy without forcing yourself to be cheerful is okay. What has been taking the biggest toll on your energy?",
                    "Having a difficult or exhausting day can leave you feeling completely drained. What feels like the most supportive thing you could do for yourself in the next couple of hours?",
                    "Navigating a difficult day takes quiet resilience. Would you like to share a little more about what made today so difficult, or would you prefer to focus on a gentle reset?"
                };
                return lowMoodTemplates[pickIndex(userMessage, lowMoodTemplates.length)];
            }

            // 17. DEFAULT EMPATHETIC REFLECTION (VARIED, NON-CLICHÉ)
            String[] defaultTemplates = {
                "I hear you. That sounds like something worth giving yourself some space to reflect on. What part of that feels most important or pressing to you right now?",
                "Thank you for sharing that. Giving words to what you're experiencing is an important first step. What would feel like the most helpful next step for you right now?",
                "I'm listening. It sounds like there are several layers to what you're experiencing. If you'd like to tell me a bit more about what led up to this, take all the time you need."
            };
            return defaultTemplates[pickIndex(userMessage, defaultTemplates.length)];
        }
    }
}
