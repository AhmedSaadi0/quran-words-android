package io.github.ahmedsaadi0.quranwords.core.util

/**
 * Static morphology reference maps and guide terms (AGENTS §14).
 * Moved from data/util — dictionary reference data with explicit AR/EN
 * variants; the DB stores raw codes only.
 */

data class MorphologyTerm(
    val code: String,
    val nameAr: String,
    val nameEn: String,
    val category: String,
    val categoryEn: String,
    val description: String,
    val descriptionEn: String,
    val exampleWord: String,
    val exampleLocation: String
)

object MorphologyMaps {
    val POS_MAP = mapOf(
        "N" to "اسم",
        "PN" to "اسم علم",
        "ADJ" to "صفة",
        "PRON" to "ضمير",
        "DEM" to "اسم إشارة",
        "REL" to "اسم موصول",
        "V" to "فعل",
        "P" to "حرف جر",
        "CONJ" to "حرف عطف",
        "SUB" to "حرف مصدري",
        "ACC" to "حرف نصب",
        "AMD" to "حرف استدراك",
        "ANS" to "حرف جواب",
        "AVR" to "حرف ردع",
        "CAUS" to "حرف سببية",
        "CERT" to "حرف تحقيق",
        "CIRC" to "واو الحال",
        "COM" to "واو المعية",
        "COND" to "أداة شرط",
        "EQ" to "همزة التسوية",
        "EXH" to "حرف تحضيض",
        "EXL" to "حرف تفصيل",
        "EXP" to "أداة استثناء",
        "FUT" to "حرف استقبال (سـ/سوف)",
        "INC" to "حرف ابتداء",
        "INT" to "حرف تفسير",
        "INTG" to "حرف استفهام",
        "NEG" to "حرف نفي",
        "PREV" to "حرف كافّة",
        "PRO" to "لام النهي",
        "REM" to "حرف استئناف",
        "RES" to "حرف حصر",
        "RET" to "حرف إضراب",
        "RSLT" to "فاء الجزاء",
        "SUP" to "حرف زائد",
        "SUR" to "حرف مفاجأة",
        "VOC" to "حرف نداء",
        "INL" to "حرف ناسخ (إن وأخواتها)",
        "VN" to "مصدر"
    )

    val FORMS_MAP = mapOf(
        "I" to "المجرد الثلاثي (فَعَلَ)",
        "II" to "التفعيل (فَعَّلَ)",
        "III" to "المفاعلة (فَاعَلَ)",
        "IV" to "الإفعال (أَفْعَلَ)",
        "V" to "التفعّل (تَفَعَّلَ)",
        "VI" to "التفاعل (تَفَاعَلَ)",
        "VII" to "الانفعال (انْفَعَلَ)",
        "VIII" to "الافتعال (افْتَعَلَ)",
        "IX" to "الافعلال (افْعَلَّ)",
        "X" to "الاستفعال (اسْتَفْعَلَ)",
        "XI" to "افعالّ (افْعَالَّ)",
        "XII" to "افعوعل (افْعَوْعَلَ)"
    )

    val ASPECT_MAP = mapOf(
        "PERF" to "ماضٍ",
        "IMPF" to "مضارع",
        "IMPV" to "أمر"
    )

    val MOOD_MAP = mapOf(
        "IND" to "مرفوع",
        "SUBJ" to "منصوب",
        "JUS" to "مجزوم"
    )

    val VOICE_MAP = mapOf(
        "ACT" to "مبني للمعلوم",
        "PASS" to "مبني للمجهول"
    )

    val CASE_MAP = mapOf(
        "NOM" to "مرفوع",
        "ACC" to "منصوب",
        "GEN" to "مجرور"
    )

    val STATE_MAP = mapOf(
        "DEF" to "معرّف",
        "INDEF" to "منكّر"
    )

    val DERIVATION_MAP = mapOf(
        "ACTPCPL" to "اسم فاعل",
        "PASSPCPL" to "اسم مفعول",
        "VN" to "مصدر صريح"
    )

    // English parallels: dictionary reference data for the English locale.
    // Arabic maps above stay the single source for Arabic; DB payloads
    // (meanings, summaries) have no English source and stay Arabic.
    val POS_MAP_EN = mapOf(
        "N" to "Noun",
        "PN" to "Proper noun",
        "ADJ" to "Adjective",
        "PRON" to "Pronoun",
        "DEM" to "Demonstrative",
        "REL" to "Relative pronoun",
        "V" to "Verb",
        "P" to "Preposition",
        "CONJ" to "Conjunction",
        "SUB" to "Subordinator",
        "ACC" to "Accusative particle",
        "AMD" to "Amendment particle",
        "ANS" to "Answer particle",
        "AVR" to "Aversion particle",
        "CAUS" to "Cause particle",
        "CERT" to "Certainty particle",
        "CIRC" to "Circumstantial waw",
        "COM" to "Comitative waw",
        "COND" to "Conditional particle",
        "EQ" to "Equalization hamza",
        "EXH" to "Exhortation particle",
        "EXL" to "Explication particle",
        "EXP" to "Exception particle",
        "FUT" to "Future particle",
        "INC" to "Inceptive particle",
        "INT" to "Explanatory particle",
        "INTG" to "Interrogative particle",
        "NEG" to "Negative particle",
        "PREV" to "Preventive particle",
        "PRO" to "Prohibitive lam",
        "REM" to "Resumption particle",
        "RES" to "Restriction particle",
        "RET" to "Retraction particle",
        "RSLT" to "Result fa",
        "SUP" to "Redundant particle",
        "SUR" to "Surprise particle",
        "VOC" to "Vocative particle",
        "INL" to "Inna particle",
        "VN" to "Verbal noun"
    )

    val FORMS_MAP_EN = mapOf(
        "I" to "Form I (فَعَلَ)",
        "II" to "Form II (فَعَّلَ)",
        "III" to "Form III (فَاعَلَ)",
        "IV" to "Form IV (أَفْعَلَ)",
        "V" to "Form V (تَفَعَّلَ)",
        "VI" to "Form VI (تَفَاعَلَ)",
        "VII" to "Form VII (انْفَعَلَ)",
        "VIII" to "Form VIII (افْتَعَلَ)",
        "IX" to "Form IX (افْعَلَّ)",
        "X" to "Form X (اسْتَفْعَلَ)",
        "XI" to "Form XI (افْعَالَّ)",
        "XII" to "Form XII (افْعَوْعَلَ)"
    )

    /** Arabic ordinals for verb forms: keeps the Arabic locale 100% Latin-free. */
    val FORM_ORDINAL_AR = mapOf(
        "I" to "الأول",
        "II" to "الثاني",
        "III" to "الثالث",
        "IV" to "الرابع",
        "V" to "الخامس",
        "VI" to "السادس",
        "VII" to "السابع",
        "VIII" to "الثامن",
        "IX" to "التاسع",
        "X" to "العاشر",
        "XI" to "الحادي عشر",
        "XII" to "الثاني عشر"
    )

    val ASPECT_MAP_EN = mapOf(
        "PERF" to "Perfect",
        "IMPF" to "Imperfect",
        "IMPV" to "Imperative"
    )

    val MOOD_MAP_EN = mapOf(
        "IND" to "Indicative",
        "SUBJ" to "Subjunctive",
        "JUS" to "Jussive"
    )

    val VOICE_MAP_EN = mapOf(
        "ACT" to "Active",
        "PASS" to "Passive"
    )

    val CASE_MAP_EN = mapOf(
        "NOM" to "Nominative",
        "ACC" to "Accusative",
        "GEN" to "Genitive"
    )

    val STATE_MAP_EN = mapOf(
        "DEF" to "Definite",
        "INDEF" to "Indefinite"
    )

    val DERIVATION_MAP_EN = mapOf(
        "ACTPCPL" to "Active participle",
        "PASSPCPL" to "Passive participle",
        "VN" to "Verbal noun"
    )

    val MORPHOLOGY_TERMS = listOf(
        MorphologyTerm("N", "الاسم", "Noun", "أقسام الكلم", "Word classes", "كلمة تدل على معنى في نفسها غير مقترن بزمان", "A word indicating a meaning in itself, not bound to time", "كِتَابٌ", "2:2"),
        MorphologyTerm("V", "الفعل", "Verb", "أقسام الكلم", "Word classes", "كلمة تدل على حدث مقترن بزمان (ماضٍ، مضارع، أمر)", "A word indicating an event bound to time (past, imperfect, imperative)", "يَعْلَمُونَ", "2:13"),
        MorphologyTerm("P", "حرف الجر", "Preposition", "أقسام الكلم", "Word classes", "حرف يربط الأسماء بالأفعال أو بأسماء أخرى ويجر ما بعده", "A particle linking nouns to verbs or to other nouns, putting its complement in the genitive", "فِي", "2:2"),
        MorphologyTerm("PRON", "الضمير", "Pronoun", "الأسماء والضمائر", "Nouns and pronouns", "اسم يدل على متكلم أو مخاطب أو غائب يقوم مقام الاسم الظاهر", "Stands for speaker, addressee or absentee in place of the explicit noun", "هُمْ", "2:5"),
        MorphologyTerm("DEM", "اسم الإشارة", "Demonstrative", "الأسماء والضمائر", "Nouns and pronouns", "ما وضع لمشار إليه حسي أو معنوي مقروناً بإشارة إليه", "Points to a sensory or abstract referent", "ذَٰلِكَ", "2:2"),
        MorphologyTerm("REL", "الاسم الموصول", "Relative pronoun", "الأسماء والضمائر", "Nouns and pronouns", "اسم مبهم لا يتعين المراد منه إلا بجملة بعده تسمى صلة الموصول", "An ambiguous noun specified only by a following clause", "الَّذِينَ", "2:3"),
        MorphologyTerm("VN", "المصدر", "Verbal noun", "المشتقات والمصادر", "Derivatives and masadirs", "اسم يدل على الحدث مجرداً من الزمان وهو أصل المشتقات", "A noun denoting the event abstracted from time; the root of derivatives", "هُدًى", "2:2"),
        MorphologyTerm("ACTPCPL", "اسم الفاعل", "Active participle", "المشتقات والمصادر", "Derivatives and masadirs", "اسم مشتق للدلالة على من قام بالفعل أو اتصف به", "A derivative denoting the doer of the act", "مُفْلِحُونَ", "2:5"),
        MorphologyTerm("PASSPCPL", "اسم المفعول", "Passive participle", "المشتقات والمصادر", "Derivatives and masadirs", "اسم مشتق للدلالة على من وقع عليه الفعل", "A derivative denoting the recipient of the act", "مَغْضُوبِ", "1:7"),
        MorphologyTerm("I", "المجرد الثلاثي", "Form I", "أبواب الأفعال", "Verb forms", "أصل الأفعال الثلاثية المجردة دون زيادة (فَعَلَ، فَعِلَ، فَعُلَ)", "Base triliteral verbs without augmentation", "كَتَبَ", "2:183"),
        MorphologyTerm("II", "التفعيل", "Form II", "أبواب الأفعال", "Verb forms", "مزيد بالتضعيف للتعدية أو التكثير والمبالغة (فَعَّلَ يُفَعِّلُ)", "Augmented by doubling, for causation or intensity", "نَزَّلَ", "2:23"),
        MorphologyTerm("III", "المفاعلة", "Form III", "أبواب الأفعال", "Verb forms", "مزيد بالألف للمشاركة أو المغالبة (فَاعَلَ يُفَاعِلُ)", "Augmented with alif, for participation", "يُخَادِعُونَ", "2:9"),
        MorphologyTerm("IV", "الإفعال", "Form IV", "أبواب الأفعال", "Verb forms", "مزيد بالهمزة في أوله للتعدية أو الدخول في الشيء (أَفْعَلَ)", "Augmented with initial hamza, for causation", "أَنزَلَ", "2:4"),
        MorphologyTerm("V", "التفعّل", "Form V", "أبواب الأفعال", "Verb forms", "مزيد بالتاء والتضعيف للمطاوعة أو التكلف (تَفَعَّلَ)", "Reflexive of Form II", "يَتَذَكَّرُونَ", "2:221"),
        MorphologyTerm("VI", "التفاعل", "Form VI", "أبواب الأفعال", "Verb forms", "مزيد بالتاء والألف للمشاركة بين اثنين فأكثر (تَفَاعَلَ)", "Mutual action between two or more", "تَعَاوَنُوا", "5:2"),
        MorphologyTerm("VII", "الانفعال", "Form VII", "أبواب الأفعال", "Verb forms", "مزيد بالنون والألف للمطاوعة ولا يكون إلا لازماً (انْفَعَلَ)", "Reflexive and always intransitive", "انفَجَرَتْ", "2:60"),
        MorphologyTerm("VIII", "الافتعال", "Form VIII", "أبواب الأفعال", "Verb forms", "مزيد بالهمزة والتاء للطلب أو الاجتهاد (افْتَعَلَ)", "Reflexive, for effort or acquisition", "اشْتَرَوْا", "2:16"),
        MorphologyTerm("X", "الاستفعال", "Form X", "أبواب الأفعال", "Verb forms", "مزيد بالهمزة والسين والتاء للطلب أو التحول (اسْتَفْعَلَ)", "Seeking or transformation", "اسْتَوْقَدَ", "2:17")
    )
}
