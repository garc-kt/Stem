package com.stem.engine


interface LanguageRules {
    val typoDictionary: Map<String, String>
    val wordyPhrases: Map<String, String>
    val formalReplacements: Map<String, String>
    val friendlyReplacements: Map<String, String>
    val punchyStarters: Map<String, String>
    val punchyWords: Map<String, String>
    val phrasalVerbGuards: Map<String, Set<String>>
    val abbreviationPattern: Regex
    fun applyLanguageSpecificFixes(text: String): String = text
}

object EnglishRules : LanguageRules {
    override val typoDictionary = mapOf(
        "teh" to "the", "recieved" to "received", "seperate" to "separate",
        "definately" to "definitely", "untill" to "until", "truely" to "truly",
        "accomodate" to "accommodate", "occured" to "occurred", "tommorow" to "tomorrow",
        "alot" to "a lot", "beleive" to "believe", "goverment" to "government",
        "calender" to "calendar", "thier" to "their", "wierd" to "weird",
        "writting" to "writing", "embarass" to "embarrass", "adress" to "address",
        "recommand" to "recommend", "neccessary" to "necessary", "succesful" to "successful",
        "availible" to "available", "completly" to "completely", "peice" to "piece",
        "noone" to "no one", "alread" to "already", "dont" to "don't",
        "cant" to "can't", "wont" to "won't", "isnt" to "isn't",
        "didnt" to "didn't", "couldnt" to "couldn't", "shouldnt" to "shouldn't",
        "wouldnt" to "wouldn't", "thats" to "that's", "whats" to "what's",
        "theres" to "there's", "lets" to "let's", "havent" to "haven't",
        "hasnt" to "hasn't", "arent" to "aren't", "werent" to "weren't",
        "u" to "you", "ur" to "your", "pls" to "please", "plz" to "please",
        "rn" to "right now", "tbh" to "honestly", "idk" to "I don't know",
        "imo" to "in my opinion", "cuz" to "because", "cos" to "because",
        "bcoz" to "because", "prolly" to "probably", "lemme" to "let me",
        "gimme" to "give me", "tryna" to "trying to", "would of" to "would have",
        "could of" to "could have", "should of" to "should have"
    )

    override val wordyPhrases = mapOf(
        "in order to" to "to", "due to the fact that" to "because",
        "at this point in time" to "now", "at the present time" to "now",
        "for the purpose of" to "to", "with regard to" to "regarding",
        "in the event that" to "if", "has the ability to" to "can",
        "is able to" to "can", "in spite of the fact that" to "although",
        "take into consideration" to "consider", "make a decision" to "decide",
        "give consideration to" to "consider", "a large number of" to "many",
        "a majority of" to "most", "at all times" to "always",
        "in close proximity to" to "near", "prior to" to "before",
        "subsequent to" to "after", "by means of" to "by",
        "in terms of" to "regarding", "as a matter of fact" to "in fact",
        "it is important to note that" to "note that", "each and every" to "every",
        "first and foremost" to "first", "basic fundamentals" to "fundamentals",
        "future plans" to "plans", "completely eliminate" to "eliminate",
        "absolutely essential" to "essential", "very unique" to "unique",
        "as per your request" to "as requested", "reach a consensus" to "agree"
    )

    override val formalReplacements = mapOf(
        "wanna" to "would like to", "gonna" to "will", "gotta" to "need to",
        "kinda" to "somewhat", "sorta" to "somewhat", "dunno" to "do not know",
        "btw" to "by the way", "asap" to "as soon as possible",
        "fyi" to "for your information", "thx" to "thank you", "thanks" to "thank you",
        "hey" to "hello", "yeah" to "yes", "yep" to "yes", "nope" to "no",
        "talk about" to "discuss", "give" to "provide", "fix" to "resolve",
        "help" to "assist", "ask" to "inquire", "buy" to "purchase",
        "get" to "obtain", "show" to "demonstrate", "tell" to "inform",
        "start" to "commence", "end" to "conclude", "make sure" to "ensure",
        "look into" to "investigate", "set up" to "configure",
        "find out" to "determine", "let me know" to "please advise",
        "sorry for" to "I apologize for", "can't" to "cannot", "won't" to "will not",
        "don't" to "do not", "didn't" to "did not", "couldn't" to "could not",
        "shouldn't" to "should not", "wouldn't" to "would not",
        "it's" to "it is", "that's" to "that is", "there's" to "there is",
        "what's" to "what is", "we're" to "we are", "they're" to "they are",
        "I'm" to "I am"
    )

    override val friendlyReplacements = mapOf(
        "obtain" to "get", "provide" to "give", "resolve" to "fix",
        "assist" to "help", "inquire" to "ask", "purchase" to "buy",
        "demonstrate" to "show", "inform" to "tell", "commence" to "start",
        "conclude" to "wrap up", "ensure" to "make sure", "investigate" to "look into",
        "configure" to "set up", "determine" to "find out",
        "please advise" to "let me know", "I apologize for" to "sorry about",
        "cannot" to "can't", "will not" to "won't", "do not" to "don't",
        "did not" to "didn't", "regarding" to "about"
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
        "good" to "great", "bad" to "critical", "fast" to "rapid",
        "big" to "massive", "nice" to "superb", "important" to "vital",
        "hard" to "challenging", "very good" to "stellar",
        "really nice" to "exceptional", "make better" to "supercharge"
    )

    override val phrasalVerbGuards = mapOf(
        "get" to setOf("up", "together", "back", "along", "over", "out", "through", "going", "ready", "started", "down", "by", "around", "away", "into")
    )

    override val abbreviationPattern =
        Regex("\\b(?:e\\.g\\.|i\\.e\\.|etc\\.|vs\\.|Dr\\.|Mr\\.|Mrs\\.|Ms\\.|Inc\\.|U\\.S\\.)", RegexOption.IGNORE_CASE)
}

object SpanishRules : LanguageRules {
    override val typoDictionary = mapOf(
        "aver" to "a ver", "asique" to "así que", "deacuerdo" to "de acuerdo",
        "osea" to "o sea", "tambien" to "también", "aqui" to "aquí",
        "alli" to "allí", "aleman" to "alemán", "rapido" to "rápido",
        "facil" to "fácil", "dificil" to "difícil", "practico" to "práctico",
        "ultimo" to "último", "publico" to "público", "medico" to "médico",
        "numero" to "número", "telefono" to "teléfono", "pagina" to "página",
        "codigo" to "código", "articulo" to "artículo", "proximo" to "próximo",
        "solucion" to "solución", "informacion" to "información",
        "direccion" to "dirección", "atencion" to "atención",
        "estacion" to "estación", "cancion" to "canción", "corazon" to "corazón",
        "revision" to "revisión", "traduccion" to "traducción",
        "produccion" to "producción", "conexion" to "conexión",
        "reunion" to "reunión", "opinion" to "opinión", "razon" to "razón",
        "leccion" to "lección", "vdd" to "verdad", "xfa" to "por favor",
        "finde" to "fin de semana"
    )

    override val wordyPhrases = mapOf(
        "en el caso de que" to "si", "debido a que" to "porque",
        "a pesar de que" to "aunque", "con el fin de" to "para",
        "en relación con" to "sobre", "en la actualidad" to "actualmente",
        "hoy en día" to "actualmente", "en primer lugar" to "primero",
        "por otro lado" to "además", "es importante señalar que" to "cabe señalar que",
        "a la mayor brevedad posible" to "lo antes posible",
        "en la medida de lo posible" to "si es posible"
    )

    override val formalReplacements = mapOf(
        "q" to "que", "xq" to "porque", "pq" to "porque", "tb" to "también",
        "porfa" to "por favor", "vale" to "de acuerdo", "chevere" to "excelente",
        "guay" to "excelente", "chau" to "adiós", "ok" to "de acuerdo",
        "genial" to "excelente", "oye" to "disculpe", "avisame" to "por favor avíseme",
        "dale" to "de acuerdo"
    )

    override val friendlyReplacements = mapOf(
        "adquirir" to "conseguir", "proporcionar" to "dar", "solicitar" to "pedir",
        "finalizar" to "terminar", "iniciar" to "empezar", "asistir" to "ayudar",
        "notificar" to "avisar", "requiere" to "necesita"
    )

    override val punchyStarters = mapOf(
        "estaba pensando que tal vez podríamos" to "Hagamos esto:",
        "sería genial si pudiéramos" to "Hagamos esto:",
        "solo quería confirmar si" to "Confirmando:",
        "les escribo para informarles que" to "Aviso:",
        "en mi opinión personal" to "Honestamente,"
    )

    override val punchyWords = mapOf(
        "bueno" to "excelente", "malo" to "crítico", "rapido" to "veloz",
        "rápido" to "veloz", "grande" to "enorme", "importante" to "vital",
        "dificil" to "desafiante", "difícil" to "desafiante"
    )

    override val phrasalVerbGuards: Map<String, Set<String>> = emptyMap()

    override val abbreviationPattern =
        Regex("\\b(?:Sr\\.|Sra\\.|Srta\\.|Dr\\.|Dra\\.|etc\\.|p\\.\\s?ej\\.|Ud\\.|Uds\\.)", RegexOption.IGNORE_CASE)

    override fun applyLanguageSpecificFixes(text: String): String {
        if (text.isBlank()) return text
        val sentences = Regex("(?<=[.!?])\\s+").split(text)
        return sentences.joinToString(" ") { sentence ->
            val trimmed = sentence.trimStart()
            val leading = sentence.substring(0, sentence.length - trimmed.length)
            when {
                trimmed.isEmpty() -> sentence
                trimmed.endsWith("?") && !trimmed.startsWith("¿") -> "${leading}¿$trimmed"
                trimmed.endsWith("!") && !trimmed.startsWith("¡") -> "${leading}¡$trimmed"
                else -> sentence
            }
        }
    }
}
