package com.veggiebit.sprout.features.enhancement.data.engine.rules

/**
 * Spanish dictionaries — new; ARCHITECTURE_AND_PROCESS.md §2 Phase 2 documented Spanish
 * spelling/tone support that never actually existed in code. Deliberately modest in scope
 * (accent restoration for high-frequency words, common informal contractions, a small
 * wordy-phrase/tone list) rather than attempting full Spanish NLP — same spirit as
 * [EnglishRules]'s hand-picked dictionaries.
 */
object SpanishRules : LanguageRules {

    // Mostly accent restoration for words routinely typed without diacritics.
    override val typoDictionary = mapOf(
        "aver" to "a ver",
        "asique" to "así que",
        "deacuerdo" to "de acuerdo",
        "osea" to "o sea",
        "tambien" to "también",
        "aqui" to "aquí",
        "alli" to "allí",
        "aleman" to "alemán",
        "rapido" to "rápido",
        "facil" to "fácil",
        "dificil" to "difícil",
        "practico" to "práctico",
        "ultimo" to "último",
        "publico" to "público",
        "medico" to "médico",
        "numero" to "número",
        "telefono" to "teléfono",
        "pagina" to "página",
        "codigo" to "código",
        "articulo" to "artículo",
        "proximo" to "próximo",
        "solucion" to "solución",
        "informacion" to "información",
        "direccion" to "dirección",
        "atencion" to "atención",
        "estacion" to "estación",
        "cancion" to "canción",
        "corazon" to "corazón",
        "revision" to "revisión",
        "traduccion" to "traducción",
        "produccion" to "producción",
        "conexion" to "conexión",
        "reunion" to "reunión",
        "opinion" to "opinión",
        "razon" to "razón",
        "leccion" to "lección",
        "vdd" to "verdad",
        "xfa" to "por favor",
        "finde" to "fin de semana"
    )

    override val wordyPhrases = mapOf(
        "en el caso de que" to "si",
        "debido a que" to "porque",
        "a pesar de que" to "aunque",
        "con el fin de" to "para",
        "en relación con" to "sobre",
        "en la actualidad" to "actualmente",
        "hoy en día" to "actualmente",
        "en primer lugar" to "primero",
        "por otro lado" to "además",
        "es importante señalar que" to "cabe señalar que",
        "a la mayor brevedad posible" to "lo antes posible",
        "en la medida de lo posible" to "si es posible"
    )

    // Informal -> formal ("usted"-register), the Professional-preset direction.
    override val formalReplacements = mapOf(
        "q" to "que",
        "xq" to "porque",
        "pq" to "porque",
        "tb" to "también",
        "porfa" to "por favor",
        "vale" to "de acuerdo",
        "chevere" to "excelente",
        "guay" to "excelente",
        "chau" to "adiós",
        "ok" to "de acuerdo",
        "genial" to "excelente",
        "oye" to "disculpe",
        "avisame" to "por favor avíseme",
        "dale" to "de acuerdo"
    )

    override val friendlyReplacements = mapOf(
        "adquirir" to "conseguir",
        "proporcionar" to "dar",
        "solicitar" to "pedir",
        "finalizar" to "terminar",
        "iniciar" to "empezar",
        "asistir" to "ayudar",
        "notificar" to "avisar",
        "requiere" to "necesita"
    )

    override val punchyStarters = mapOf(
        "estaba pensando que tal vez podríamos" to "Hagamos esto:",
        "sería genial si pudiéramos" to "Hagamos esto:",
        "solo quería confirmar si" to "Confirmando:",
        "les escribo para informarles que" to "Aviso:",
        "en mi opinión personal" to "Honestamente,"
    )

    override val punchyWords = mapOf(
        "bueno" to "excelente",
        "malo" to "crítico",
        "rapido" to "veloz",
        "rápido" to "veloz",
        "grande" to "enorme",
        "importante" to "vital",
        "dificil" to "desafiante",
        "difícil" to "desafiante"
    )

    // Spanish doesn't have the English get-up/get-together phrasal-verb collision problem.
    override val phrasalVerbGuards: Map<String, Set<String>> = emptyMap()

    override val abbreviationPattern =
        Regex("\\b(?:Sr\\.|Sra\\.|Srta\\.|Dr\\.|Dra\\.|etc\\.|p\\.\\s?ej\\.|Ud\\.|Uds\\.)", RegexOption.IGNORE_CASE)

    /**
     * Restores a missing opening ¿/¡ on sentences that end in ?/! but don't already open with
     * the inverted mark — the single most common punctuation gap in casually-typed Spanish.
     */
    override fun applyLanguageSpecificFixes(text: String): String {
        if (text.isBlank()) return text
        val sentences = Regex("(?<=[.!?])\\s+").split(text)
        return sentences.joinToString(" ") { sentence ->
            val trimmed = sentence.trimStart()
            val leading = sentence.substring(0, sentence.length - trimmed.length)
            when {
                trimmed.isEmpty() -> sentence
                trimmed.endsWith("?") && !trimmed.startsWith("¿") -> "$leading¿$trimmed"
                trimmed.endsWith("!") && !trimmed.startsWith("¡") -> "$leading¡$trimmed"
                else -> sentence
            }
        }
    }
}
