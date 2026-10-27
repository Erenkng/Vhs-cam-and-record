# VHS Cam

Material 3 Expressive (M3E) arayüzlü bir Android kamera uygulaması. Arayüz moderndir; çekilen videolar VHS kaseti gibi, fotoğraflar ise retro stillerde (VHS kare, 90'lar dijital, 35mm film, Polaroid) görünür.

*An Android camera app with a modern Material 3 Expressive UI that records VHS-style video and takes retro-looking photos. Kotlin, Jetpack Compose, CameraX and OpenGL ES 3.0; English and Turkish UI.*

## Özellikler

**Stiller (looks)**
- **VHS:** Görüntü canlı işlenir; vizörde gördüğünüz kaydedilenle aynıdır. Renk taşması, satır titremesi, kafa geçiş bandı, tracking bantları, dropout ve kar; bant hızı **SP / LP / EP**, "Bant yıpranması" ve her etki için ince ayar. Camcorder tarih/saat damgası, **PLAY ▶** yazısı ve bant sayacı.
- **Dijital (CCD):** 2000'lerin başındaki dijital kameralar: doygun renkler, mor saçak, gerçek düşük kaliteli JPEG ve amber dijital tarih.
- **35mm:** 3:2, film greni, halation, ışık sızıntısı ve turuncu tarih.
- **Polaroid:** Kare kadraj, soluk renkler, kimyasal düzensizlik ve beyaz çerçeve.
- **8-bit:** 128×112 piksel, dört yeşil ton, Bayer titreşimi (dithering) ve el konsolu kamerasının çerçevesi.
- **2005 webcam:** QVGA, baş edemeyen otomatik pozlama, mavi ton, blok blok sıkıştırma; 15 fps.
- **CCTV:** Siyah-beyaz, geniş açı bozulması, kayan şebeke bandı, "CAM 01" ve saniyeli zaman damgası; 5 fps.
- VHS, 8-bit, webcam ve CCTV video da çeker; hepsi fotoğraf çeker. Her stilin yoğunluk ayarı vardır.

**Camcorder düğmeleri (VHS)**
- **Efekt tuşu:** Gece görüşü (NightShot), sepya, siyah-beyaz, mozaik, strobe ve hareket izi.
- **Dijital zoom:** Lensin bittiği yerden 10 kata kadar; büyüdükçe bloklaşır. Vizörde W–T çubuğu ve **D.ZOOM** görünür.
- **Fader:** Kayıt başlarken ve sürerken açılır, dururken ve duraklarken kararır; siyaha ya da beyaza.
- **VCR mavi ekranı:** Kasetler bir saniyelik mavi ekranla başlar; kamera görüntüsü gelene kadar vizörde "NO SIGNAL" yazar.
- **Aralıklı çekim:** 10 sn, 30 sn ya da 1 dakikada bir saniyelik kayıt.
- Kayıt duraklatılıp sürdürülebilir; **kaset sesi** sese wow, flutter ve hışırtı ekler. 4:3 veya 16:9; SD, HD ya da Full HD.

**Modlar**
- **Pro:** ISO, enstantane hızı, pozlama telafisi (EV), netleme mesafesi ve beyaz dengesi cetvel kadranlarıyla elle ayarlanır; otomatikte kalan ayarlar kameranın seçtiği değeri gösterir. Yalnızca ISO ya da yalnızca enstantane seçilirse diğeri parlaklığı koruyacak şekilde hesaplanır.
- **Fotoğraf** ve **Video**; destekleyen telefonlarda **Çift çekim:** ön ve arka kamera aynı anda, resim içinde resim.
- **Fotokabin:** Geri sayımla dört kare, tarihli tek bir fotoğraf şeridinde.

**Sonrası**
- **Retro GIF:** Kasetin izlediğiniz yerinden 3 saniyelik, web-safe paletli bir GIF.
- **Sonradan stil değiştirme:** Fotoğrafın orijinali saklanır; istediğiniz stil ve yoğunlukla yeni bir fotoğraf olarak yeniden banyo edilir. Ayarlardan kapatılabilir ve saklanan orijinaller silinebilir.
- **Kasetler** galerisi: görüntüleyici, yakınlaştırma, video oynatma, paylaşma ve silme.

**Kamera kontrolleri**
- Dokunarak netleme ve ölçüm, uzun basınca **AE/AF KİLİT**; pozlama slider'ı; iki parmakla ve hazır düğmelerle zoom.
- Manuel netlemede netlik kenarları pembe vurgulanır (focus peaking; kayda girmez).
- Flaş ve fener, 3 ve 10 saniyelik zamanlayıcı, ızgara, ön/arka kamera, ses ya da kamera tuşuyla çekim.

**Arayüz**
- Telefonun kendi kamerası gibi: üstte ikon çubuğu, kenardan kenara vizör, altta stil küçük resimleri, mod şeridi ve deklanşör.
- Material 3 Expressive bileşenleri, yaylı animasyonlar, şekil değiştiren deklanşör, dinamik renk ve titreşimli geri bildirim.
- İlk açılışta tanıtım ekranı; açılışta kaset animasyonlu açılış ekranı ve eski bir televizyon gibi açılan görüntü.
- Türkçe ve İngilizce.

Çekilen dosyalar `DCIM/VHS Cam` klasörüne kaydedilir ve telefonun galeri uygulamasında da görünür.

## Kurulum

Gereksinimler: Android 10 (API 29) veya üstü, OpenGL ES 3.0 destekleyen bir telefon.

**Sürümler:** Depodaki **Releases** bölümünden son sürümün `VHS-Cam-vX.Y.apk` dosyasını telefona indirip açın; gerekirse "bilinmeyen kaynaklardan yükleme" iznini verin. Sürüm APK'ları aynı anahtarla imzalanır, bu yüzden yeni sürümler eskisinin üzerine kurulur.

**Geliştirme derlemeleri:** Her CI çalışması bir debug APK da üretir. **Actions** sekmesinde **Android CI** iş akışının son yeşil çalışmasını açın ve **Artifacts** bölümünden **vhs-cam-debug-apk** dosyasını indirin. Debug APK'lar her çalışmada farklı bir anahtarla imzalanır; birinden diğerine geçerken önce eskisini kaldırmanız gerekebilir.

### Yeni sürüm çıkarmak

1. `app/build.gradle.kts` içinde `versionCode` ve `versionName` değerlerini artırın; isterseniz `docs/release-notes/vX.Y.md` dosyasına sürüm notlarını yazın.
2. `vX.Y` biçiminde bir etiket gönderin (`git tag v0.6 && git push origin v0.6`) ya da GitHub'da **Actions → Release → Run workflow** ile etiketi yazıp çalıştırın; telefondan da yapılabilir.
3. **Release** iş akışı testleri çalıştırır, APK'yı sürüm anahtarıyla imzalar ve GitHub'da sürümü yayınlar.

İmza anahtarı depoda değil, iki depo sırrında (Settings → Secrets and variables → Actions) durur: `SIGNING_KEYSTORE_BASE64` (PKCS#12 anahtar deposunun base64 hâli, takma ad `vhscam`) ve `SIGNING_PASSWORD`. Bu sırlar yoksa iş akışı sürümü yalnızca o çalışmada var olan tek seferlik bir anahtarla imzalar: APK yine kurulur, ama sonraki sürüm onun üzerine güncelleme olarak kurulamaz. Kalıcı anahtarı kullanıyorsanız bir yedeğini güvenli bir yerde saklayın: sonraki sürümler aynı anahtarla imzalanmazsa Android onları eski sürümün üzerine kurmaz.

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
- [ ] **Yeni stiller:** 8-bit, webcam ve CCTV ile fotoğraf ve video; 8-bit fotoğrafta çerçeve, webcam ve CCTV videosunda düşük kare hızı.
- [ ] **Efektler:** Efekt tuşundaki her efekt vizörde ve kayıtta görünmeli; strobe ve iz hareketle belli olmalı.
- [ ] **Dijital zoom:** Lensin sonundan sonra zoom sürmeli, W–T çubuğu ve D.ZOOM görünmeli.
- [ ] **Fader ve mavi ekran:** Kayıt mavi ekranla başlamalı, fader açıkken başta/sonda ve duraklatmada kararma olmalı.
- [ ] **Aralıklı çekim:** Kaydedilen video bir saniyelik parçalardan oluşmalı; rozet geri sayımı göstermeli.
- [ ] **Çift çekim:** Destekleyen telefonda iki kamera birlikte görünmeli; küçük görüntünün yeri ve yönü doğru olmalı.
- [ ] **Pro:** ISO ve enstantane değiştikçe parlaklık değişmeli; yalnızca biri seçilince parlaklık korunmalı; fotoğraf aynı ayarlarla kaydedilmeli.
- [ ] **Fotokabin, GIF ve stil değiştirme:** Dört karelik şerit; videodan GIF; eski bir fotoğrafın stilini değiştirip yeni fotoğraf olarak kaydetme.
- [ ] **İlk açılış:** Tanıtım ekranı bir kez görünmeli; açılış animasyonu ve televizyon gibi açılma akıcı olmalı.

## Lisanslar

Yazı tipleri SIL Open Font License ile dağıtılır: VT323 (ekran yazıları) ve Roboto Flex (arayüz). İkonlar Material Symbols'tır (Apache 2.0). Lisans metinleri `app/src/main/assets/licenses/` klasöründedir ve uygulamada **Ayarlar → Lisanslar** ekranında görünür.
