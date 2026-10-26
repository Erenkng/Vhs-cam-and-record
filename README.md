# VHS Cam

Material 3 Expressive (M3E) arayüzlü bir Android kamera uygulaması. Arayüz moderndir; çekilen videolar VHS kaseti gibi, fotoğraflar ise retro stillerde (VHS kare, 90'lar dijital, 35mm film, Polaroid) görünür.

*An Android camera app with a modern Material 3 Expressive UI that records VHS-style video and takes retro-looking photos. Kotlin, Jetpack Compose, CameraX and OpenGL ES 3.0; English and Turkish UI.*

## Özellikler

**VHS video**
- Görüntü canlı olarak işlenir; vizörde gördüğünüz, kaydedilenle aynıdır.
- VHS etkileri: renk taşması ve kayması, yumuşak parlaklık ve keskinleştirme halesi, satır titremesi, alttaki kafa geçiş bandı, kayan tracking bantları, dropout ve kar.
- Bant hızı **SP / LP / EP** (EP en yıpranmışı) ve "Bant yıpranması" ayarı. "İnce ayar" bölümünde her etki ayrı ayrı ayarlanabilir.
- Camcorder tarih ve saat damgası görüntüye işlenir. Tarih biçimi ve 12/24 saat seçilebilir; "Kamera saati" ile istediğiniz bir tarihe (ör. 1997) ayarlanabilir.
- İsteğe bağlı **PLAY ▶** yazısı ve bant sayacı.
- Kayıt duraklatılıp sürdürülebilir; kayıt başlarken ve devam ederken kısa bir "kaset kesme" bozulması oluşur.
- **Kaset sesi:** Kayıttan sonra sese wow, flutter, hışırtı ve dar bant genişliği eklenir. Ayarlardan kapatılabilir.
- 4:3 veya 16:9 oran; SD, HD veya Full HD kayıt.

**Fotoğraf stilleri**
- **VHS:** Videodaki kare, 4:3 ya da 16:9.
- **Dijital (CCD):** 2000'lerin başındaki dijital kameralar. Doygun renkler, patlayan parlak alanlar, mor saçak, gerçek düşük kaliteli JPEG sıkıştırması ve amber renkli dijital tarih.
- **35mm:** 3:2 oran, film greni, halation, solmuş siyahlar, her karede farklı ışık sızıntısı ve turuncu yedi segmentli tarih.
- **Polaroid:** Kare kadraj, soluk ve yumuşak renkler, kimyasal düzensizlik ve beyaz çerçeve.
- Her stilin bir yoğunluk ayarı vardır. Ön kamerada "Selfie'leri aynala" seçeneği bulunur.

**Kamera kontrolleri**
- Dokunarak netleme ve ölçüm; uzun basınca **AE/AF KİLİT**.
- Pozlama (EV) slider'ı.
- İki parmakla zoom ve hazır zoom düğmeleri (0.5×, 1×, 2×, 5×; cihaz destekliyorsa).
- **Pro:** Manuel netleme (kullanılırken netlik kenarları pembe vurgulanır, buna focus peaking denir; kayda girmez) ve beyaz dengesi hazır ayarları.
- Fotoğrafta flaş (kapalı, otomatik, açık), videoda fener.
- 3 ve 10 saniyelik zamanlayıcı, ızgara, ön/arka kamera.
- Ses tuşlarıyla ya da kamera tuşuyla çekim.

**Arayüz (Material 3 Expressive)**
- Yüzen araç çubuğu, bağlı (connected) düğme grupları ve yaylı animasyonlar.
- Şekil değiştiren deklanşör: basınca kurabiye şekline girer, kayıtta kırmızı daire "dur" karesine dönüşür ve arkasında bir makara döner.
- Dinamik renk (Android 12+), titreşimli geri bildirim ve deklanşör sesi.
- **Kasetler** galerisi: fotoğraf ve video görüntüleyici, yakınlaştırma, paylaşma ve silme.

Çekilen dosyalar `DCIM/VHS Cam` klasörüne kaydedilir ve telefonun galeri uygulamasında da görünür.

## Kurulum

Gereksinimler: Android 10 (API 29) veya üstü, OpenGL ES 3.0 destekleyen bir telefon.

**Sürümler:** Depodaki **Releases** bölümünden son sürümün `VHS-Cam-vX.Y.apk` dosyasını telefona indirip açın; gerekirse "bilinmeyen kaynaklardan yükleme" iznini verin. Sürüm APK'ları aynı anahtarla imzalanır, bu yüzden yeni sürümler eskisinin üzerine kurulur.

**Geliştirme derlemeleri:** Her CI çalışması bir debug APK da üretir. **Actions** sekmesinde **Android CI** iş akışının son yeşil çalışmasını açın ve **Artifacts** bölümünden **vhs-cam-debug-apk** dosyasını indirin. Debug APK'lar her çalışmada farklı bir anahtarla imzalanır; birinden diğerine geçerken önce eskisini kaldırmanız gerekebilir.

### Yeni sürüm çıkarmak

1. `app/build.gradle.kts` içinde `versionCode` ve `versionName` değerlerini artırın; isterseniz `docs/release-notes/vX.Y.md` dosyasına sürüm notlarını yazın.
2. `vX.Y` biçiminde bir etiket gönderin (`git tag v0.6 && git push origin v0.6`).
3. **Release** iş akışı testleri çalıştırır, APK'yı sürüm anahtarıyla imzalar ve GitHub'da sürümü yayınlar.

İmza anahtarı depoda değil, iki depo sırrında (Settings → Secrets and variables → Actions) durur: `SIGNING_KEYSTORE_BASE64` (PKCS#12 anahtar deposunun base64 hâli, takma ad `vhscam`) ve `SIGNING_PASSWORD`. Anahtarın bir yedeğini güvenli bir yerde saklayın: sonraki sürümler aynı anahtarla imzalanmazsa Android onları eski sürümün üzerine kurmaz.

## Derleme ve testler

JDK 17 veya üstü ve Android SDK gerekir; CI JDK 21 kullanır.

```bash
./gradlew :app:assembleDebug             # debug APK: app/build/outputs/apk/debug/
./gradlew -p core test                   # DSP, yön matematiği, tarih ve hazır ayar testleri (saf JVM)
./gradlew :app:testDebugUnitTest         # uygulama birim testleri
./gradlew :app:recordRoborazziDebug      # ekran görüntüleri: app/build/outputs/roborazzi/
./gradlew :app:lintDebug
```

Shader'ları telefonsuz denemek için `tools/shader-lab`, uygulamadaki GLSL dosyalarının aynısını tarayıcıda (WebGL2) çalıştırır:

```bash
npm i -D playwright && npx playwright install chromium
node tools/shader-lab/render.mjs görsel-klasörü çıktı-klasörü
```

## Proje yapısı

| Klasör | İçerik |
|---|---|
| `core/` | Android'den bağımsız Kotlin: yön matematiği, stil hazır ayarları, kaset sesi DSP'si, tarih damgası biçimleri |
| `app/src/main/assets/shaders/` | GLSL ES 3.00 geçişleri: VHS (renk, bant, sunum), CCD, film, halation, Polaroid |
| `app/.../camera/` | CameraX oturumu, netleme ve pozlama, fotoğraf ve video işleme hatları |
| `app/.../gl/` | EGL, `SurfaceProcessor` (canlı efekt), `LookRenderer`, fotoğraf için `StillRenderer`, ekran yazıları |
| `app/.../audio/` | Media3 Transformer ile kaset sesi |
| `app/.../ui/` | Compose arayüzü: kamera, galeri, ayarlar, izin ekranı |
| `tools/shader-lab/` | Shader deneme ortamı |

## Cihaz test listesi

Kamera ve GPU davranışı yalnızca gerçek cihazda doğrulanabilir. Yeni bir sürümü denerken:

- [ ] **Yön:** Arka ve ön kamerayla, telefon dik ve yan tutulurken video çekin. Tarama çizgileri ve titreme izleyiciye göre yatay olmalı; tarih damgası sol altta ve okunur olmalı (ön kamerada ters yazı olmamalı).
- [ ] **Fotoğraf stilleri:** Dört stille, dik ve yan tutarak çekin. Kaydedilen fotoğrafın yönü ve kırpması vizörle aynı olmalı. Polaroid'de çerçeve olmalı; ön kamerada "Selfie'leri aynala" ayarı çalışmalı.
- [ ] **Netleme:** Dokununca halka "netlendi" ya da "başarısız" göstermeli. Uzun basınca AE/AF KİLİT çipi çıkmalı; çipe dokununca kilit kalkmalı.
- [ ] **Pozlama ve zoom:** EV slider'ı parlaklığı değiştirmeli; iki parmakla ve hazır düğmelerle zoom çalışmalı.
- [ ] **Pro:** Manuel netleme slider'ı netliği değiştirmeli ve pembe kenar vurgusu görünmeli; kaydedilen videoda bu vurgu olmamalı. Beyaz dengesi hazır ayarları renkleri değiştirmeli.
- [ ] **Flaş ve fener:** Fotoğrafta flaş modları, videoda fener çalışmalı.
- [ ] **Zamanlayıcı ve tuşlar:** 3 ve 10 saniyelik geri sayım; ses tuşuyla çekim.
- [ ] **Kayıt:** Duraklat ve devam et (devamda kısa bozulma); 4:3/16:9 ve SD/HD/FHD seçenekleri.
- [ ] **Ses:** Kaset sesi açıkken ve kapalıyken kayıt; mikrofon izni verilmeden kayıt (sessiz video).
- [ ] **Kayıp olmamalı:** Kayıt ya da kaset sesi işlenirken uygulamayı arka plana alın veya kapatın; video yine Kasetler'de görünmeli.
- [ ] **Kamera saati:** Özel bir tarih ayarlayın; damgada o tarih görünmeli ve saat ilerlemeye devam etmeli.
- [ ] **Galeri:** Fotoğraf yakınlaştırma, video oynatma, paylaşma ve silme.
- [ ] **Akıcılık:** Düşük seviye bir telefonda vizör ve kayıt yaklaşık 30 fps olmalı.
- [ ] **Ayarlar:** Duvar kâğıdı renkleri, titreşim ve deklanşör sesi anahtarları.

## Lisanslar

Yazı tipleri SIL Open Font License ile dağıtılır: VT323 (ekran yazıları) ve Roboto Flex (arayüz). İkonlar Material Symbols'tır (Apache 2.0). Lisans metinleri `app/src/main/assets/licenses/` klasöründedir ve uygulamada **Ayarlar → Lisanslar** ekranında görünür.
