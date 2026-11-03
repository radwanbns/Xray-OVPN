# OpenVpnPayload

تطبيق أندرويد بسيط بلغة Kotlin يدعم:
- حقن Payload مخصص (WebSocket style)
- هيكل جاهز لإضافة OpenVPN لاحقاً

## الـ Payload المستخدم

```
GET / HTTP/1.1\r\n
Host: [host]\r\n
Upgrade: Websocket\r\n
Connection: Keep-Alive\r\n
User-Agent: [ua]\r\n
Upgrade: websocket\r\n
\r\n
```

## كيفية الاستخدام

1. افتح المشروع فـ Android Studio
2. غير `applicationId` إلا بغيتي
3. الـ Local Proxy كيخدم على بورت `1080` افتراضياً
4. تقدر تربط أي OpenVPN client يخدم عبر HTTP Proxy على `127.0.0.1:1080`

## إضافة OpenVPN لاحقاً

الأفضل تستعمل مكتبة:
- [ics-openvpn](https://github.com/schwabe/ics-openvpn)

## ملاحظات مهمة

- هذا مشروع تعليمي / أساس للتطوير
- ماشي تطبيق جاهز للإنتاج
- خاصك تختبر الـ Payload مع الشبكة ديالك
- استعملو بمسؤولية

## الملفات المهمة

- `PayloadProxy.kt` → كيحقن الـ Payload
- `OpenVpnService.kt` → هيكل VpnService
- `MainActivity.kt` → واجهة بسيطة
