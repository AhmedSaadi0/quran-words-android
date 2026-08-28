package com.example.data.util

data class SurahMeta(
    val id: Int,
    val nameAr: String,
    val nameEn: String,
    val ayahCount: Int,
    val revelationType: String,
    val juzStart: Int
)

data class JuzMeta(
    val id: Int,
    val nameAr: String,
    val startSurahId: Int,
    val startAyah: Int,
    val description: String
)

data class MorphologyTerm(
    val code: String,
    val nameAr: String,
    val category: String,
    val description: String,
    val exampleWord: String,
    val exampleLocation: String
)

object QuranMetaConstants {
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

    val MORPHOLOGY_TERMS = listOf(
        MorphologyTerm("N", "الاسم (Noun)", "أقسام الكلم", "كلمة تدل على معنى في نفسها غير مقترن بزمان", "كِتَابٌ", "2:2"),
        MorphologyTerm("V", "الفعل (Verb)", "أقسام الكلم", "كلمة تدل على حدث مقترن بزمان (ماضٍ، مضارع، أمر)", "يَعْلَمُونَ", "2:13"),
        MorphologyTerm("P", "حرف الجر (Preposition)", "أقسام الكلم", "حرف يربط الأسماء بالأفعال أو بأسماء أخرى ويجر ما بعده", "فِي", "2:2"),
        MorphologyTerm("PRON", "الضمير (Pronoun)", "الأسماء والضمائر", "اسم يدل على متكلم أو مخاطب أو غائب يقوم مقام الاسم الظاهر", "هُمْ", "2:5"),
        MorphologyTerm("DEM", "اسم الإشارة (Demonstrative)", "الأسماء والضمائر", "ما وضع لمشار إليه حسي أو معنوي مقروناً بإشارة إليه", "ذَٰلِكَ", "2:2"),
        MorphologyTerm("REL", "الاسم الموصول (Relative)", "الأسماء والضمائر", "اسم مبهم لا يتعين المراد منه إلا بجملة بعده تسمى صلة الموصول", "الَّذِينَ", "2:3"),
        MorphologyTerm("VN", "المصدر (Verbal Noun)", "المشتقات والمصادر", "اسم يدل على الحدث مجرداً من الزمان وهو أصل المشتقات", "هُدًى", "2:2"),
        MorphologyTerm("ACTPCPL", "اسم الفاعل (Active Participle)", "المشتقات والمصادر", "اسم مشتق للدلالة على من قام بالفعل أو اتصف به", "مُفْلِحُونَ", "2:5"),
        MorphologyTerm("PASSPCPL", "اسم المفعول (Passive Participle)", "المشتقات والمصادر", "اسم مشتق للدلالة على من وقع عليه الفعل", "مَغْضُوبِ", "1:7"),
        MorphologyTerm("I", "المجرد الثلاثي (Form I)", "أبواب الأفعال", "أصل الأفعال الثلاثية المجردة دون زيادة (فَعَلَ، فَعِلَ، فَعُلَ)", "كَتَبَ", "2:183"),
        MorphologyTerm("II", "التفعيل (Form II)", "أبواب الأفعال", "مزيد بالتضعيف للتعدية أو التكثير والمبالغة (فَعَّلَ يُفَعِّلُ)", "نَزَّلَ", "2:23"),
        MorphologyTerm("III", "المفاعلة (Form III)", "أبواب الأفعال", "مزيد بالألف للمشاركة أو المغالبة (فَاعَلَ يُفَاعِلُ)", "يُخَادِعُونَ", "2:9"),
        MorphologyTerm("IV", "الإفعال (Form IV)", "أبواب الأفعال", "مزيد بالهمزة في أوله للتعدية أو الدخول في الشيء (أَفْعَلَ)", "أَنزَلَ", "2:4"),
        MorphologyTerm("V", "التفعّل (Form V)", "أبواب الأفعال", "مزيد بالتاء والتضعيف للمطاوعة أو التكلف (تَفَعَّلَ)", "يَتَذَكَّرُونَ", "2:221"),
        MorphologyTerm("VI", "التفاعل (Form VI)", "أبواب الأفعال", "مزيد بالتاء والألف للمشاركة بين اثنين فأكثر (تَفَاعَلَ)", "تَعَاوَنُوا", "5:2"),
        MorphologyTerm("VII", "الانفعال (Form VII)", "أبواب الأفعال", "مزيد بالنون والألف للمطاوعة ولا يكون إلا لازماً (انْفَعَلَ)", "انفَجَرَتْ", "2:60"),
        MorphologyTerm("VIII", "الافتعال (Form VIII)", "أبواب الأفعال", "مزيد بالهمزة والتاء للطلب أو الاجتهاد (افْتَعَلَ)", "اشْتَرَوْا", "2:16"),
        MorphologyTerm("X", "الاستفعال (Form X)", "أبواب الأفعال", "مزيد بالهمزة والسين والتاء للطلب أو التحول (اسْتَفْعَلَ)", "اسْتَوْقَدَ", "2:17")
    )

    val SURAHS = listOf(
        SurahMeta(1, "الفاتحة", "Al-Fatihah", 7, "مكية", 1),
        SurahMeta(2, "البقرة", "Al-Baqarah", 286, "مدنية", 1),
        SurahMeta(3, "آل عمران", "Ali 'Imran", 200, "مدنية", 3),
        SurahMeta(4, "النساء", "An-Nisa", 176, "مدنية", 4),
        SurahMeta(5, "المائدة", "Al-Ma'idah", 120, "مدنية", 6),
        SurahMeta(6, "الأنعام", "Al-An'am", 165, "مكية", 7),
        SurahMeta(7, "الأعراف", "Al-A'raf", 206, "مكية", 8),
        SurahMeta(8, "الأنفال", "Al-Anfal", 75, "مدنية", 10),
        SurahMeta(9, "التوبة", "At-Tawbah", 129, "مدنية", 10),
        SurahMeta(10, "يونس", "Yunus", 109, "مكية", 11),
        SurahMeta(11, "هود", "Hud", 123, "مكية", 11),
        SurahMeta(12, "يوسف", "Yusuf", 111, "مكية", 12),
        SurahMeta(13, "الرعد", "Ar-Ra'd", 43, "مدنية", 13),
        SurahMeta(14, "إبراهيم", "Ibrahim", 52, "مكية", 13),
        SurahMeta(15, "الحجر", "Al-Hijr", 99, "مكية", 14),
        SurahMeta(16, "النحل", "An-Nahl", 128, "مكية", 14),
        SurahMeta(17, "الإسراء", "Al-Isra", 111, "مكية", 15),
        SurahMeta(18, "الكهف", "Al-Kahf", 110, "مكية", 15),
        SurahMeta(19, "مريم", "Maryam", 98, "مكية", 16),
        SurahMeta(20, "طه", "Taha", 135, "مكية", 16),
        SurahMeta(21, "الأنبياء", "Al-Anbiya", 112, "مكية", 17),
        SurahMeta(22, "الحج", "Al-Hajj", 78, "مدنية", 17),
        SurahMeta(23, "المؤمنون", "Al-Mu'minun", 118, "مكية", 18),
        SurahMeta(24, "النور", "An-Nur", 64, "مدنية", 18),
        SurahMeta(25, "الفرقان", "Al-Furqan", 77, "مكية", 18),
        SurahMeta(26, "الشعراء", "Ash-Shu'ara", 227, "مكية", 19),
        SurahMeta(27, "النمل", "An-Naml", 93, "مكية", 20),
        SurahMeta(28, "القصص", "Al-Qasas", 88, "مكية", 20),
        SurahMeta(29, "العنكبوت", "Al-'Ankabut", 69, "مكية", 20),
        SurahMeta(30, "الروم", "Ar-Rum", 60, "مكية", 21),
        SurahMeta(31, "لقمان", "Luqman", 34, "مكية", 21),
        SurahMeta(32, "السجدة", "As-Sajdah", 30, "مكية", 21),
        SurahMeta(33, "الأحزاب", "Al-Ahzab", 73, "مدنية", 21),
        SurahMeta(34, "سبأ", "Saba", 54, "مكية", 22),
        SurahMeta(35, "فاطر", "Fatir", 45, "مكية", 22),
        SurahMeta(36, "يس", "Ya-Sin", 83, "مكية", 22),
        SurahMeta(37, "الصافات", "As-Saffat", 182, "مكية", 23),
        SurahMeta(38, "ص", "Sad", 88, "مكية", 23),
        SurahMeta(39, "الزمر", "Az-Zumar", 75, "مكية", 23),
        SurahMeta(40, "غافر", "Ghafir", 85, "مكية", 24),
        SurahMeta(41, "فصلت", "Fussilat", 54, "مكية", 24),
        SurahMeta(42, "الشورى", "Ash-Shura", 53, "مكية", 25),
        SurahMeta(43, "الزخرف", "Az-Zukhruf", 89, "مكية", 25),
        SurahMeta(44, "الدخان", "Ad-Dukhan", 59, "مكية", 25),
        SurahMeta(45, "الجاثية", "Al-Jathiyah", 37, "مكية", 25),
        SurahMeta(46, "الأحقاف", "Al-Ahqaf", 35, "مكية", 26),
        SurahMeta(47, "محمد", "Muhammad", 38, "مدنية", 26),
        SurahMeta(48, "الفتح", "Al-Fath", 29, "مدنية", 26),
        SurahMeta(49, "الحجرات", "Al-Hujurat", 18, "مدنية", 26),
        SurahMeta(50, "ق", "Qaf", 45, "مكية", 26),
        SurahMeta(51, "الذاريات", "Adh-Dhariyat", 60, "مكية", 26),
        SurahMeta(52, "الطور", "At-Tur", 49, "مكية", 27),
        SurahMeta(53, "النجم", "An-Najm", 62, "مكية", 27),
        SurahMeta(54, "القمر", "Al-Qamar", 55, "مكية", 27),
        SurahMeta(55, "الرحمن", "Ar-Rahman", 78, "مدنية", 27),
        SurahMeta(56, "الواقعة", "Al-Waqi'ah", 96, "مكية", 27),
        SurahMeta(57, "الحديد", "Al-Hadid", 29, "مدنية", 27),
        SurahMeta(58, "المجادلة", "Al-Mujadila", 22, "مدنية", 28),
        SurahMeta(59, "الحشر", "Al-Hashr", 24, "مدنية", 28),
        SurahMeta(60, "الممتحنة", "Al-Mumtahanah", 13, "مدنية", 28),
        SurahMeta(61, "الصف", "As-Saff", 14, "مدنية", 28),
        SurahMeta(62, "الجمعة", "Al-Jumu'ah", 11, "مدنية", 28),
        SurahMeta(63, "المنافقون", "Al-Munafiqun", 11, "مدنية", 28),
        SurahMeta(64, "التغابن", "At-Taghabun", 18, "مدنية", 28),
        SurahMeta(65, "الطلاق", "At-Talaq", 12, "مدنية", 28),
        SurahMeta(66, "التحريم", "At-Tahrim", 12, "مدنية", 28),
        SurahMeta(67, "الملك", "Al-Mulk", 30, "مكية", 29),
        SurahMeta(68, "القلم", "Al-Qalam", 52, "مكية", 29),
        SurahMeta(69, "الحاقة", "Al-Haqqah", 28, "مكية", 29),
        SurahMeta(70, "المعارج", "Al-Ma'arij", 44, "مكية", 29),
        SurahMeta(71, "نوح", "Nuh", 28, "مكية", 29),
        SurahMeta(72, "الجن", "Al-Jinn", 28, "مكية", 29),
        SurahMeta(73, "المزمل", "Al-Muzzammil", 20, "مكية", 29),
        SurahMeta(74, "المدثر", "Al-Muddaththir", 56, "مكية", 29),
        SurahMeta(75, "القيامة", "Al-Qiyamah", 40, "مكية", 29),
        SurahMeta(76, "الإنسان", "Al-Insan", 31, "مدنية", 29),
        SurahMeta(77, "المرسلات", "Al-Mursalat", 50, "مكية", 29),
        SurahMeta(78, "النبأ", "An-Naba", 40, "مكية", 30),
        SurahMeta(79, "النازعات", "An-Nazi'at", 46, "مكية", 30),
        SurahMeta(80, "عبس", "'Abasa", 42, "مكية", 30),
        SurahMeta(81, "التكوير", "At-Takwir", 17, "مكية", 30),
        SurahMeta(82, "الانفطار", "Al-Infitar", 19, "مكية", 30),
        SurahMeta(83, "المطففين", "Al-Mutaffifin", 36, "مكية", 30),
        SurahMeta(84, "الانشقاق", "Al-Inshiqaq", 25, "مكية", 30),
        SurahMeta(85, "البروج", "Al-Buruj", 22, "مكية", 30),
        SurahMeta(86, "الطارق", "At-Tariq", 17, "مكية", 30),
        SurahMeta(87, "الأعلى", "Al-A'la", 19, "مكية", 30),
        SurahMeta(88, "الغاشية", "Al-Ghashiyah", 26, "مكية", 30),
        SurahMeta(89, "الفجر", "Al-Fajr", 30, "مكية", 30),
        SurahMeta(90, "البلد", "Al-Balad", 20, "مكية", 30),
        SurahMeta(91, "الشمس", "Ash-Shams", 15, "مكية", 30),
        SurahMeta(92, "الليل", "Al-Layl", 21, "مكية", 30),
        SurahMeta(93, "الضحى", "Ad-Duha", 11, "مكية", 30),
        SurahMeta(94, "الشرح", "Ash-Sharh", 8, "مكية", 30),
        SurahMeta(95, "التين", "At-Tin", 8, "مكية", 30),
        SurahMeta(96, "العلق", "Al-'Alaq", 19, "مكية", 30),
        SurahMeta(97, "القدر", "Al-Qadr", 5, "مكية", 30),
        SurahMeta(98, "البينة", "Al-Bayyinah", 8, "مدنية", 30),
        SurahMeta(99, "الزلزلة", "Az-Zalzalah", 8, "مدنية", 30),
        SurahMeta(100, "العاديات", "Al-'Adiyat", 11, "مكية", 30),
        SurahMeta(101, "القارعة", "Al-Qari'ah", 11, "مكية", 30),
        SurahMeta(102, "التكاثر", "At-Takathur", 8, "مكية", 30),
        SurahMeta(103, "العصر", "Al-'Asr", 3, "مكية", 30),
        SurahMeta(104, "الهمزة", "Al-Humazah", 9, "مكية", 30),
        SurahMeta(105, "الفيل", "Al-Fil", 5, "مكية", 30),
        SurahMeta(106, "قريش", "Quraysh", 4, "مكية", 30),
        SurahMeta(107, "الماعون", "Al-Ma'un", 7, "مكية", 30),
        SurahMeta(108, "الكوثر", "Al-Kawthar", 3, "مكية", 30),
        SurahMeta(109, "الكافرون", "Al-Kafirun", 6, "مكية", 30),
        SurahMeta(110, "النصر", "An-Nasr", 3, "مدنية", 30),
        SurahMeta(111, "المسد", "Al-Masad", 5, "مكية", 30),
        SurahMeta(112, "الإخلاص", "Al-Ikhlas", 4, "مكية", 30),
        SurahMeta(113, "الفلق", "Al-Falaq", 5, "مكية", 30),
        SurahMeta(114, "الناس", "An-Nas", 6, "مكية", 30)
    )

    val JUZ_LIST = (1..30).map { juzNum ->
        val startSurah = SURAHS.firstOrNull { it.juzStart == juzNum } ?: SURAHS[0]
        JuzMeta(
            id = juzNum,
            nameAr = "الجزء $juzNum",
            startSurahId = startSurah.id,
            startAyah = 1,
            description = "يبدأ بسورة ${startSurah.nameAr}"
        )
    }

    const val STATS_UNIQUE_WORDS = 21295
    const val STATS_WORD_POSITIONS = 77429
    const val STATS_VERIFIED_ROOTS = 1642
    const val STATS_MASADIR = 5273
    const val STATS_DERIVATIVES = 16245
    const val STATS_AYAT = 6236
    const val STATS_SURAHS = 114
}
