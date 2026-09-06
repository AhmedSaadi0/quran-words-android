# إصدارات قاعدة البيانات (مضغوطة ZIP + manifest)

مصدر الحقيقة: **Releases في `AhmedSaadi0/quran-words`** — القاعدة الحالية = `v1` (رمز 1).

## 1. بنية الإصدار

كل إصدار `db-vN` يحوي asset واحدًا باسم ثابت:

- `quran_words.db.zip` — بداخله ملف واحد في الجذر: `quran_words.db` (بدون مجلدات)
- `data/db-manifest.json` على `main` يُحدَّث ليشير للإصدار الأحدث (الفحص السريع ~1KB)

مثال `db-manifest.json`:

```json
{
  "latestVersionCode": 2,
  "latestVersionName": "v2",
  "downloadUrl": "https://github.com/AhmedSaadi0/quran-words/releases/download/db-v2/quran_words.db.zip",
  "compressedSize": 28000000,
  "uncompressedSize": 118534144,
  "sha256": "<hex64 of the zip>",
  "publishedAt": "2026-09-05T00:00:00Z",
  "minAppVersionCode": 3,
  "releaseNotesAr": ["إضافة أجزاء الآية", "إصلاح المعاني"],
  "releasePageUrl": "https://github.com/AhmedSaadi0/quran-words/releases/tag/db-v2"
}
```

ملاحظات الإصدار مصفوفة نصوص (بند لكل سطر) — التطبيق يجمعها بـ `\n`: البانر يعرض أول ~3 أسطر، وشاشة الإدارة تعرض الكل. الشكل القديم (نص واحد) ما زال مقبولًا كتوافق رجعي.

قواعد: `latestVersionCode` عدد صحيح يتزايد فقط (المقارنة به لا بالنص)، الرابط `https://` فقط، `sha256` بصمة ملف الـ ZIP (64 hex) — التطبيق يرفض الملف عند عدم التطابق.

## 2. إصدار نسخة جديدة (في مستودع البيانات)

```bash
# من مستودع AhmedSaadi0/quran-words:
sqlite3 data/quran_words.db "VACUUM;"
zip -9 -j quran_words.db.zip data/quran_words.db
unzip -l quran_words.db.zip   # يجب أن يظهر quran_words.db واحدًا فقط
sha256sum quran_words.db.zip
# حدّث data/db-manifest.json بالأرقام والبصمة وملاحظات الإصدار، ثم:
gh release create db-v2 quran_words.db.zip --title "DB v2" --notes "ملاحظات الإصدار بالعربية"
```

سكربت مساعد يولّد الـ ZIP والبصمة ومقطع الـ manifest: `scripts/make-db-release.sh` (في مستودع التطبيق، يُشغَّل من مستودع البيانات بتمرير المسارات).

## 3. سلوك التطبيق

- أول تثبيت: شاشة الإدارة تعرض معلومات الإصدار من الـ manifest ثم تنزيل مضغوط (~28MB بدل 118MB) + فك ضغط داخل الجهاز مع شريط مرحلتين.
- مستخدم حالي بلا نسخة مخزنة + ملف `>50MB`: يُرحَّل تلقائيًا إلى `1/v1` ولا يُعرض له تحديث وهمي.
- بانر الرئيسية `db_update_banner` يظهر فقط عند `latestCode > installedCode` ولم يضغط "لاحقًا" على نفس النسخة. الفحص صامت عند انقطاع الشبكة.
- الاستيراد اليدوي يقبل `.db` أو `.zip` (كشف بالـ magic bytes) مع نفس التحقق (ترويسة SQLite + الحجم).
- `quran_words.db` مستبعد من النسخ الاحتياطي (`data_extraction_rules.xml` + `backup_rules.xml`).
- مسار `.db` الخام القديم (118MB غير مضغوط) محذوف نهائيًا — ZIP فقط.
