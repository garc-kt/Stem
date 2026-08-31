package com.veggiebit.sprout.features.enhancement.data.engine.rules

/**
 * English dictionaries — moved out of LocalRuleEngine verbatim (same word lists it always had)
 * so language selection could be introduced without changing the existing English behavior.
 */
object EnglishRules : LanguageRules {

    override val typoDictionary = mapOf(
        "teh" to "the",
        "recieved" to "received",
        "seperate" to "separate",
        "definately" to "definitely",
        "untill" to "until",
        "truely" to "truly",
        "accomodate" to "accommodate",
        "occured" to "occurred",
        "tommorow" to "tomorrow",
        "alot" to "a lot",
        "beleive" to "believe",
        "goverment" to "government",
        "calender" to "calendar",
        "thier" to "their",
        "wierd" to "weird",
        "writting" to "writing",
        "embarass" to "embarrass",
        "adress" to "address",
        "recommand" to "recommend",
        "neccessary" to "necessary",
        "succesful" to "successful",
        "availible" to "available",
        "completly" to "completely",
        "peice" to "piece",
        "noone" to "no one",
        "alread" to "already",
        "dont" to "don't",
        "cant" to "can't",
        "wont" to "won't",
        "isnt" to "isn't",
        "didnt" to "didn't",
        "couldnt" to "couldn't",
        "shouldnt" to "shouldn't",
        "wouldnt" to "wouldn't",
        "thats" to "that's",
        "whats" to "what's",
        "theres" to "there's",
        "lets" to "let's",
        "havent" to "haven't",
        "hasnt" to "hasn't",
        "arent" to "aren't",
        "werent" to "weren't"
    )

    override val wordyPhrases = mapOf(
        "in order to" to "to",
        "due to the fact that" to "because",
        "at this point in time" to "now",
        "at the present time" to "now",
        "for the purpose of" to "to",
        "with regard to" to "regarding",
        "in the event that" to "if",
        "has the ability to" to "can",
        "is able to" to "can",
        "in spite of the fact that" to "although",
        "take into consideration" to "consider",
        "make a decision" to "decide",
        "give consideration to" to "consider",
        "a large number of" to "many",
        "a majority of" to "most",
        "at all times" to "always",
        "in close proximity to" to "near",
        "prior to" to "before",
        "subsequent to" to "after",
        "by means of" to "by",
        "in terms of" to "regarding",
        "as a matter of fact" to "in fact",
        "it is important to note that" to "note that",
        "each and every" to "every",
        "first and foremost" to "first",
        "basic fundamentals" to "fundamentals",
        "future plans" to "plans",
        "completely eliminate" to "eliminate",
        "absolutely essential" to "essential",
        "very unique" to "unique",
        "as per your request" to "as requested",
        "reach a consensus" to "agree"
    )

    override val formalReplacements = mapOf(
        "wanna" to "would like to",
        "gonna" to "will",
        "gotta" to "need to",
        "kinda" to "somewhat",
        "sorta" to "somewhat",
        "dunno" to "do not know",
        "btw" to "by the way",
        "asap" to "as soon as possible",
        "fyi" to "for your information",
        "thx" to "thank you",
        "thanks" to "thank you",
        "hey" to "hello",
        "yeah" to "yes",
        "yep" to "yes",
        "nope" to "no",
        "talk about" to "discuss",
        "give" to "provide",
        "fix" to "resolve",
        "help" to "assist",
        "ask" to "inquire",
        "buy" to "purchase",
        "get" to "obtain",
        "show" to "demonstrate",
        "tell" to "inform",
        "start" to "commence",
        "end" to "conclude",
        "make sure" to "ensure",
        "look into" to "investigate",
        "set up" to "configure",
        "find out" to "determine",
        "let me know" to "please advise",
        "sorry for" to "I apologize for",
        "can't" to "cannot",
        "won't" to "will not",
        "don't" to "do not",
        "didn't" to "did not",
        "couldn't" to "could not",
        "shouldn't" to "should not",
        "wouldn't" to "would not",
        "it's" to "it is",
        "that's" to "that is",
        "there's" to "there is",
        "what's" to "what is",
        "we're" to "we are",
        "they're" to "they are",
        "I'm" to "I am"
    )

    // Curated rather than a mechanical reverse of formalReplacements — reversing that map
    // wholesale pulls in text-speak ("thank you" -> "thx") that reads as sloppy, not warm.
    override val friendlyReplacements = mapOf(
        "obtain" to "get",
        "provide" to "give",
        "resolve" to "fix",
        "assist" to "help",
        "inquire" to "ask",
        "purchase" to "buy",
        "demonstrate" to "show",
        "inform" to "tell",
        "commence" to "start",
        "conclude" to "wrap up",
        "ensure" to "make sure",
        "investigate" to "look into",
        "configure" to "set up",
        "determine" to "find out",
        "please advise" to "let me know",
        "I apologize for" to "sorry about",
        "cannot" to "can't",
        "will not" to "won't",
        "do not" to "don't",
        "did not" to "didn't",
        "regarding" to "about"
    )

    override val punchyStarters = mapOf(
        "i was thinking that maybe we could" to "Let's",
        "it would be great if we could" to "Let's",
        "we might want to consider" to "Let's",
        "there are many reasons why" to "Key reasons:",
        "i just wanted to check if" to "Checking in:",
        "i am writing this to let you know" to "Update:",
        "in my personal opinion" to "Honestly,",
        "feel free to" to "Please"
    )

    override val punchyWords = mapOf(
        "good" to "great",
        "bad" to "critical",
        "fast" to "rapid",
        "big" to "massive",
        "nice" to "superb",
        "important" to "vital",
        "hard" to "challenging",
        "very good" to "stellar",
        "really nice" to "exceptional",
        "make better" to "supercharge"
    )

    override val phrasalVerbGuards = mapOf(
        "get" to setOf("up", "together", "back", "along", "over", "out", "through", "going", "ready", "started", "down", "by", "around", "away", "into")
    )

    override val abbreviationPattern =
        Regex("\\b(?:e\\.g\\.|i\\.e\\.|etc\\.|vs\\.|Dr\\.|Mr\\.|Mrs\\.|Ms\\.|Inc\\.|U\\.S\\.)", RegexOption.IGNORE_CASE)
}
