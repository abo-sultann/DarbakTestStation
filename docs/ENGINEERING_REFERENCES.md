# Engineering References

هذه المراجع للاستفادة الهندسية قبل إعادة تنفيذ الوظائف من الصفر. لا يُنسخ كود منها تلقائياً؛ يجب مراجعة الترخيص والتوافق مع Android 7.1/API 25 أولاً.

## 1. AndroidApkAnalyzer — MartinStyk
مرجع أساسي لفحص APK والتطبيقات: metadata, min/target SDK, permissions, components, signing, ABI/native libraries وقراءة ملفات APK غير المثبتة.
ملاحظة: الإصدار الحالي يتطلب Android 9، لذلك نستفيد من الأفكار والبنية فقط ونحافظ نحن على API 25.
License: GPL-3.0.

## 2. AnotherMonitor — AntonioRedondo
مرجع مهم لمراقبة CPU/RAM وتسجيل القياسات، والأهم توثيقه لقيود /proc على Android 7/8.
License: GPL-3.0.

## 3. atop — chandradeep24
مرجع خفيف لمراقبة الأداء: CPU/RAM/FPS، يدعم API 24+ ولا يحتاج root للوظائف الأساسية.
License: MIT.

## 4. ProcessLens — Dreamucxe
مرجع لمبدأ بالغ الأهمية: لا نخمن القياسات المقيدة. كل قراءة يجب أن تكون Value أو Restricted/Unavailable أو Failed مع توضيح السبب.
الإصدار الحالي Android 8+؛ نستفيد من النموذج المنطقي فقط.
License: MIT.

## قرارات Darbak Test Station
- APK Inspector يتوسع لاحقاً إلى ABI/signing/components/permissions.
- كل metric يوضح هل هو متاح فعلاً أم مقيد، ولا تعرض قيمة تقديرية كأنها حقيقية.
- الأداء الأساسي يستهدف API 25 بدون Root.
- الميزات التي تحتاج ADB/Shizuku/Root تبقى طبقة اختيارية لاحقاً.
- نجاح Acer لا يساوي اعتماد T3.
