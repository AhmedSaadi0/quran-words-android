package com.quranwords.core

import com.quranwords.core.util.VerseReferenceParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VerseReferenceParserTest {
    @Test fun westernReference() = assertEquals(2 to 255, VerseReferenceParser.parse("2:255"))
    @Test fun easternReference() = assertEquals(2 to 255, VerseReferenceParser.parse("٢:٢٥٥"))
    @Test fun spaceSeparator() = assertEquals(2 to 255, VerseReferenceParser.parse("2 255"))
    @Test fun arabicName() = assertEquals(2 to 255, VerseReferenceParser.parse("البقرة 255"))
    @Test fun withSurahPrefix() = assertEquals(18 to 10, VerseReferenceParser.parse("سورة الكهف 10"))
    @Test fun easternName() = assertEquals(2 to 255, VerseReferenceParser.parse("البقرة ٢٥٥"))
    @Test fun compoundSurah() = assertEquals(3 to 18, VerseReferenceParser.parse("آل عمران 18"))
    @Test fun compoundWithPrefix() = assertEquals(3 to 18, VerseReferenceParser.parse("سورة آل عمران 18"))
    @Test fun yaSin() = assertEquals(36 to 1, VerseReferenceParser.parse("يس 1"))
    @Test fun invalidSurah() = assertNull(VerseReferenceParser.parse("115:1"))
    @Test fun invalidAyahBeyondCount() = assertNull(VerseReferenceParser.parse("1:8"))
    @Test fun barePhraseNoNumberIsNull() = assertNull(VerseReferenceParser.parse("لا أقسم بهذا البلد"))
    @Test fun blankIsNull() = assertNull(VerseReferenceParser.parse("   "))
    @Test fun textWithoutNumberIsNull() = assertNull(VerseReferenceParser.parse("البقرة"))
}
