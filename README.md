# Zehra Müzik — Android Studio kaynak projesi

Bu proje, Zehra Müzik için hazırlanmış **reklam SDK'sı içermeyen**, Kotlin + Jetpack Compose tabanlı premium arayüz başlangıç projesidir.

## İçerik
- Premium koyu tema, mor ve altın vurgular
- Ana sayfa, keşfet/arama, tür filtresi, favoriler ve çalma listesi ekranları
- Örnek parçalar üzerinden arayüz etkileşimleri
- Android Studio / Gradle yapılandırması
- Reklam SDK'sı veya reklam ağı bağımlılığı yoktur

## Önemli sınırlama
Bu, **çalışan müzik servisiyle tamamlanmış bir ürün değil**, kaynak kodu ve etkileşimli demo arayüzüdür. Örnek parça isimleri yer tutucudur; gerçek ses dosyası veya yayın URL'si içermez. Gerçek oynatma, arka plan servisleri, kilit ekranı kontrolleri, kalıcı favoriler/çalma listeleri ve sunucu entegrasyonu için kullanma hakkınız olan katalog/yayın adresleri ve ayrıca geliştirme gerekir.

MYT Müzik Online APK'sının özel sunucu adresleri, verileri veya kodu bu projeye kopyalanmamıştır.

## Açma ve derleme
1. ZIP dosyasını çıkarın.
2. Android Studio'da **Open** ile `ZehraMuzik_AndroidStudio` klasörünü açın.
3. JDK 17 ve Android SDK Platform 35 kurulu olsun.
4. Gradle senkronizasyonunun bitmesini bekleyin.
5. `app` yapılandırmasını bir Android cihazda veya emülatörde çalıştırın.

İlk Gradle senkronizasyonu için internet bağlantısı gerekir.


## Gerçek yayın oynatma (eklenen)
- AndroidX Media3 ExoPlayer ile HTTP/HTTPS canlı ses akışı oynatımı.
- Türkiye istasyon kataloğu Radio Browser API üzerinden uygulama açılışında yüklenir.
- Arama, istasyon seçimi, oynat/duraklat ve yenileme kontrolleri.
- HLS için Media3 HLS modülü eklenmiştir.
- Uygulama, yayın sesini kendi sunucusunda barındırmaz; doğrudan istasyonun yayın URL'sine bağlanır.

### Yayın ve haklar
Radio Browser dizinindeki istasyon verileri yayın URL'lerinin telif veya her ülkede dinleme hakkını garanti etmez. Her istasyonun yayın koşulları, bölgesel kısıtlamaları ve marka/logo kullanım hakları ayrı olabilir. Katalogdaki istasyonlar üçüncü taraf yayıncılara aittir; bazı adresler çevrimdışı olabilir veya değişebilir.

### Bu sürümün sınırları
Bu başlangıç sürümünde oynatma uygulama açıkken çalışır. Arka planda kesintisiz oynatma, bildirim/kilit ekranı kontrolleri, favorilerin kalıcı kaydı ve uygulama kapanınca devam etme henüz eklenmemiştir. Gerçek müzik dosyası kataloğu değil, canlı internet radyo akışları sağlar. Akış başarısı ağ ve yayıncıya bağlıdır.


## İndirme altyapısı (eklenen)
- Android sistem `DownloadManager` ile arka planda dosya indirme.
- İndirme durumlarını, ilerleme yüzdesini ve alınan/toplam byte miktarını gösteren İndirme Merkezi.
- İndirmeyi kaldırma/iptal etme ve dosyaları ortak `Music/Zehra Müzik` klasörüne kaydetme.
- Ana ekrandaki indirme simgesinden merkeze erişim.
- Doğrudan HTTP/HTTPS dosya URL'leri içindir; canlı radyo akışları parça dosyası olmadığından bu ekrandan indirilebilir şarkı olarak sunulmaz.

### Eski MYT Müzik API'si hakkında
İncelenen MYT APK'sında `http://mp3.lavibu.com/` tabanı ve `api.php?song_download_id=`, `api.php?mp3_id=` gibi uç nokta adları görünüyor. Ancak API yanıt şeması, kimlik doğrulama gereksinimleri ve indirme URL'sinin nasıl üretildiği APK'daki dizelerden tek başına doğrulanamadı. Bu nedenle eski özel API'ye tahmini istekler eklenmedi; bunun yerine çalışabilir, Android sistem indirme kuyruğunu kullanan genel dosya indirme altyapısı eklendi. Eski servis entegrasyonu için yetkili API dokümantasyonu veya örnek yanıt gerekir.
