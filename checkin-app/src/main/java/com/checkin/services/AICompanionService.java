package com.checkin.services;

import com.checkin.model.AnalysisResult;
import com.checkin.model.ChatMessage;

import java.util.*;

/**
 * Service orchestrating the AI Companion conversation session for EmoSense.
 *
 * Implements a structured, empathetic, pattern-based supportive reflection engine.
 * Transparently indicates it is an academic prototype rather than pretending to be
 * an external generative LLM API, while providing modular architecture for future model integration.
 */
public class AICompanionService {

    private final List<ChatMessage> conversationHistory = new ArrayList<>();
    private AnalysisResult latestContext;
    private final CompanionEngine engine;

    public AICompanionService() {
        this(new PatternBasedCompanionEngine());
    }

    public AICompanionService(CompanionEngine engine) {
        this.engine = engine != null ? engine : new PatternBasedCompanionEngine();
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
     * Generates and appends a companion response based on the current conversation history.
     */
    public ChatMessage generateCompanionResponse() {
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
        String replyText = engine.generateReply(prompt, Collections.unmodifiableList(conversationHistory), latestContext);
        ChatMessage companionMsg = ChatMessage.companion(replyText);
        conversationHistory.add(companionMsg);
        return companionMsg;
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

    /**
     * Modular interface allowing future integration of real LLMs or remote APIs.
     */
    public interface CompanionEngine {
        String generateReply(String userMessage, List<ChatMessage> history, AnalysisResult context);
    }

    /**
     * Production-ready Java-based supportive reflection engine adhering to EmoSense guidelines:
     * - Empathetic and conversational
     * - Non-judgmental and supportive
     * - Strict medical/psychiatric diagnosis prevention
     * - Safety crisis resources for self-harm keywords
     * - Session context and multi-turn memory
     */
    public static class PatternBasedCompanionEngine implements CompanionEngine {

        @Override
        public String generateReply(String userMessage, List<ChatMessage> history, AnalysisResult context) {
            String lower = userMessage.toLowerCase().trim();

            // 1. SAFETY & CRISIS GUARDRAIL
            if (containsAny(lower, "suicide", "kill myself", "end my life", "harm myself", "hurt myself", "don't want to live", "want to die")) {
                return "I hear that you're going through a deeply overwhelming and painful moment. Because your safety matters, please consider connecting with someone who can support you right now. You can talk with a trusted friend, family member, or reach out to a support service such as a local crisis line (dial or text 988 in the US/Canada, or contact your local emergency services). You don't have to carry this alone, and supportive people are available to help.";
            }

            // 2. MEDICAL / PSYCHIATRIC DIAGNOSIS GUARDRAIL
            boolean isDiagnosisInquiry = containsAny(lower,
                    "diagnose", "diagnosis", "clinical depression", "anxiety disorder",
                    "bipolar", "mentally ill", "mental illness", "psychiatry", "psychiatric",
                    "medical opinion", "medical advice"
            ) || (containsAny(lower, "do i have", "am i suffering", "am i diagnosed") &&
                    containsAny(lower, "depression", "anxiety", "disorder", "illness", "condition"));

            if (isDiagnosisInquiry) {
                String signalPart = "";
                if (context != null && context.getCombinedLabel() != null) {
                    signalPart = "Your recent check-in shows a " + context.getCombinedLabel().toLowerCase() + " emotional signal, but this is an observational indicator rather than a medical assessment. ";
                } else if (context != null && context.hasFacial()) {
                    signalPart = "Your recent check-in shows a " + context.getFacialLabel().toLowerCase() + "-related emotional signal, but this is an observational indicator rather than a medical assessment. ";
                }
                return "EmoSense provides emotional reflections, not medical or psychological diagnosis. " + signalPart +
                        "If you are experiencing persistent emotional distress or have questions about mental health conditions, we strongly encourage speaking with a qualified healthcare or mental health professional.";
            }

            // 3. CHECK-IN QUERY
            if (containsAny(lower, "my check-in", "what did my check-in say", "my check in", "check-in results", "check in results", "what was my emotion")) {
                if (context != null && (context.hasFacial() || context.hasText() || context.getCombinedLabel() != null)) {
                    StringBuilder desc = new StringBuilder("Based on your latest check-in: ");
                    if (context.getCombinedLabel() != null) {
                        desc.append("the combined emotional insight indicated a ").append(context.getCombinedLabel()).append(". ");
                    } else if (context.hasFacial()) {
                        desc.append("the facial signal observed was ").append(context.getFacialLabel()).append(". ");
                    }
                    if (context.hasText()) {
                        desc.append("Your textual emotional cue was ").append(context.getTextLabel()).append(". ");
                    }
                    desc.append("How does that reflection match up with what you are experiencing inside right now?");
                    return desc.toString();
                } else {
                    return "You haven't submitted a check-in in this session yet. You can start a check-in anytime from the Check-In screen, or feel free to share what is on your mind right now.";
                }
            }

            // 4. MULTI-TURN CONVERSATION MEMORY: Contextual follow-up
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

                // Follow-up to "What made it difficult?" or difficult day reflection:
                if (previousAssistant != null && (previousAssistant.contains("what made it difficult") || previousAssistant.contains("tell me more about what happened") || previousAssistant.contains("weighing on you"))) {
                    if (containsAny(lower, "college", "work", "studies", "classes", "university", "assignment", "exam", "school", "academics")) {
                        return "That makes sense. Having a lot of college work can feel overwhelming. What part felt hardest today?";
                    }
                    if (containsAny(lower, "friends", "friend", "argument", "relationship", "partner", "people", "fight", "broke up")) {
                        return "Navigating relationship tensions or social misunderstandings can take a heavy emotional toll. Would you like to share a little more about what happened?";
                    }
                }
            }

            // 5. POSITIVE / ACHIEVEMENT / CELEBRATION
            boolean isAchievement = containsAny(lower,
                    "won", "winner", "winning", "first place", "1st place", "gold medal",
                    "champion", "championship", "competition", "tournament", "race",
                    "aced", "passed my", "got an a", "got into", "promoted", "promotion",
                    "achieved", "achievement", "succeeded", "success", "accomplished", "accomplishment",
                    "did really well", "nailed it", "crushed it", "worked hard for"
            );

            if (isAchievement) {
                if (containsAny(lower, "running competition", "running", "race", "marathon", "sprint", "athletics", "track")) {
                    return "That's wonderful! Winning a running competition is something to be proud of. You put in the effort and it paid off. How are you feeling about the achievement?";
                }
                if (containsAny(lower, "college", "exam", "test", "assignment", "grade", "gpa", "aced", "passed", "graduated", "degree", "scholarship")) {
                    return "Congratulations on that fantastic achievement! Dedicating yourself to your studies and seeing that hard work pay off is a huge milestone. How does it feel to celebrate this success?";
                }
                return "That is an incredible achievement! When you work hard toward something and succeed, it's worth taking time to celebrate and feel proud of yourself. What part of this success feels most rewarding to you?";
            }

            // 6. JOY / EXCITEMENT
            if (containsAny(lower, "happy", "excited", "good day", "great day", "joy", "joyful", "proud", "wonderful", "relieved", "celebrate", "amazing", "thrilled", "fantastic", "elated", "can't believe i won")) {
                return "I'm so glad to hear that! Taking a moment to acknowledge and celebrate positive moments is a wonderful practice. What was the best part of your day?";
            }

            // 7. SADNESS / DIFFICULT DAY / LOW MOOD
            if (containsAny(lower, "difficult day", "hard day", "tough day", "rough day", "bad day", "awful day", "terrible day", "sad", "sadness", "down", "unhappy", "crying", "heartbroken", "depressed", "hurting", "gloomy", "feeling low", "miserable", "blue")) {
                return "I'm sorry your day felt difficult. Would you like to tell me what made it difficult?";
            }

            // 8. FRUSTRATION / ANGER / IRRITATION
            if (containsAny(lower, "frustrated", "frustrating", "frustration", "angry", "mad", "pissed", "furious", "annoyed", "annoying", "irritated", "upset with", "hate this", "unfair")) {
                return "Frustration can be intense, especially when situations feel unfair or outside your control. What feels like the biggest source of that frustration right now?";
            }

            // 9. ANXIETY / STRESS / OVERWHELMED
            if (containsAny(lower, "anxious", "worried", "nervous", "stressed", "stress", "overwhelmed", "panic", "freaking out", "dread")) {
                return "It sounds like there's a lot of pressure on you right now. When things feel overwhelming, giving yourself permission to take a few slow, steady breaths can help create a little breathing room. Would you like to explore what may have contributed to this feeling?";
            }

            // 10. FATIGUE / EXHAUSTION / BURNOUT
            if (containsAny(lower, "tired", "exhausted", "drained", "can't sleep", "sleepy", "burnout", "burned out", "no energy", "fatigue")) {
                return "It sounds like you're carrying a lot of fatigue right now. That kind of exhaustion is very real. Have you had an opportunity to rest or step away from demands today?";
            }

            // 11. COLLEGE / STUDY / ACADEMIC
            if (containsAny(lower, "college", "university", "school", "homework", "exam", "exams", "finals", "midterm", "midterms", "assignments", "assignment", "study", "studying", "deadline", "deadlines", "classwork", "professor", "too much work")) {
                return "Academic and college pressures can accumulate quickly. When you have so much work to finish, it's easy to feel overloaded. Are you able to take a short pause to catch your breath, or is there a particular task that feels especially pressing right now?";
            }

            // 12. RELATIONSHIP / SOCIAL / CONFLICT / LONELINESS
            if (containsAny(lower, "alone", "lonely", "isolated", "nobody to talk to", "left out")) {
                return "Feeling lonely can be really tough. Even when things feel quiet around you, your thoughts and experiences matter. Is there someone in your life—a trusted friend, family member, or mentor—you might feel comfortable reaching out to for a brief connection?";
            }

            if (containsAny(lower, "friend", "friends", "boyfriend", "girlfriend", "partner", "relationship", "broke up", "breakup", "ignored me", "argument", "fight with", "fighting with", "conflict", "talked behind my back")) {
                return "Navigating relationship tensions or social misunderstandings can take a heavy emotional toll. It's completely valid to feel hurt or unsettled by this. Would you like to share a little more about what happened?";
            }

            // 13. GREETINGS & PLEASANTRIES
            if (containsAny(lower, "hello", "hi", "hey", "good morning", "good evening", "good afternoon")) {
                String greeting = "Hello! I'm here to offer a calm, supportive space to reflect. How are you feeling today?";
                if (context != null && context.getCombinedLabel() != null) {
                    greeting = "Hello! I'm here to support your reflection. We noticed a " + context.getCombinedLabel().toLowerCase() + " signal in your recent check-in. How are you feeling right now?";
                }
                return greeting;
            }

            if (containsAny(lower, "thank you", "thanks", "appreciate it", "grateful")) {
                return "You're very welcome. Remember to be gentle with yourself today. I'm always here whenever you'd like to pause and reflect.";
            }

            // 14. DEFAULT EMPATHETIC REFLECTION
            return "I hear you. That sounds like something worth giving yourself a little space to reflect on. If you'd like, you can tell me more about what happened, or what might help you feel a bit more grounded right now.";
        }

        private boolean containsAny(String input, String... candidates) {
            for (String candidate : candidates) {
                if (input.contains(candidate)) {
                    return true;
                }
            }
            return false;
        }
    }
}
