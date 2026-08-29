# كلمات القرآن — تطبيق أندرويد

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android" alt="Android" />
  <img src="https://img.shields.io/badge/Compose-Material3-4285F4?style=for-the-badge&logo=jetpackcompose" alt="Compose" />
  <img src="https://img.shields.io/badge/Kotlin-2.2-7F52FF?style=for-the-badge&logo=kotlin" alt="Kotlin" />
  <img src="https://img.shields.io/badge/MinSDK-24-34A853?style=for-the-badge" alt="MinSDK" />
</p>

<p align="center">
  <b>المعجم الشامل لألفاظ القرآن — جذور، مصادر، مشتقات، وآيات مع تحليل صرفي</b><br/>
  امتداد مباشر لمشروع الويب والبيانات المفتوحة
  <a href="https://github.com/AhmedSaadi0/quran-words"><b>AhmedSaadi0/quran-words</b></a>
</p>

<p align="center">
  <a href="https://github.com/AhmedSaadi0/quran-words">🌐 نسخة الويب</a> •
  <a href="https://github.com/AhmedSaadi0/quran-words/issues">🐛 الإبلاغ عن معنى</a> •
  <a href="https://github.com/AhmedSaadi0/quran-words#readme">📚 البيانات</a>
</p>

---

### ✨ المميزات

- **معجم الجذور** — 1,642 جذر مع معاني من لسان العرب والصحاح ومقاييس اللغة
- **المصادر والمشتقات** — 5,273 مصدر و 16,245 مشتق بأوزانها
- **الآيات مع التمييز** — الكلمة المطابقة للجذر مميزة بلون داخل الآية
- **تحليل صرفي + ملخص ذكي** — لكل كلمة، مع نموذج وتاريخ التوليد
- **بحث شامل** — جذور، كلمات، مصادر، وآيات مع تطبيع عربي
- **إشارات مرجعية ومتابعة** — حفظ سور/آيات والعودة لآخر موضع بدقة
- **يعمل بدون إنترنت** — قاعدة بيانات 118 ميجابايت تُنزّل أو تُستورد يدوياً

### 🛠️ التقنية

`Kotlin` · `Compose Material3` · `Room` · `Navigation` · `DataStore` · `OkHttp`

### 🚀 التشغيل

```bash
git clone https://github.com/AhmedSaadi0/quran-words.git
# افتح مجلد android (quran-words) في Android Studio
```

1. افتح `Android Studio` → `Open` → اختر مجلد المشروع
2. انتظر `Gradle Sync`
3. شغّل على محاكي أو جهاز (Min SDK 24)
4. عند أول تشغيل حمّل قاعدة البيانات من **الرئيسية → تنزيل** أو **استيراد من الذاكرة** (`quran_words.db`)

لا حاجة لـ `.env` أو مفاتيح.

### 📥 قاعدة البيانات

- التحميل التلقائي من `media.githubusercontent.com` / `github raw`
- أو استيراد يدوي: ضع `quran_words.db` في `Download` ثم **استيراد من الذاكرة**
- المسار الداخلي: `/data/data/io.github.ahmedsaadi0.quranwords/databases/quran_words.db`

### 🤝 المساهمة

وجدت معنى ناقصاً؟ من صفحة **تفاصيل الجذر** اضغط **؟** أو كرت **الإبلاغ عن معنى** — يُفتح نموذج جاهز على GitHub.

```text
https://github.com/AhmedSaadi0/quran-words/issues/new?title=...
```

### 📄 الترخيص

بيانات المشروع مفتوحة، الكود متاح للاستفادة التعليمية. الأصل: [AhmedSaadi0/quran-words](https://github.com/AhmedSaadi0/quran-words)
