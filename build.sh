#!/bin/bash
# Builds the APK without the Android SDK: aapt2 (from apktool), javac, dx, apksig.
set -euo pipefail
cd "$(dirname "$0")"
T=/root/tools; AAPT2=$T/prebuilt/linux/aapt2_64; FW=$T/prebuilt/android-framework.jar; AJAR=$T/android-4.1.1.4.jar
B=build; rm -rf $B; mkdir -p $B/res $B/gen $B/classes $B/dex
$AAPT2 compile --dir app/res -o $B/res/res.zip
$AAPT2 link -o $B/unsigned.apk -I $FW --manifest app/AndroidManifest.xml --java $B/gen \
  --min-sdk-version 24 --target-sdk-version 34 --version-code 2 --version-name 1.1 $B/res/res.zip
javac -nowarn --release 8 -classpath $AJAR:$T/json-20080701.jar -d $B/classes \
  $(find app/src $B/gen -name '*.java') 2>&1 | grep -v "^warning: \[options\]" || true
ls $B/classes/app/wizardscore/MainActivity.class >/dev/null
java -cp $T/dalvik-dx-16.0.1.jar com.android.dx.command.Main --dex --min-sdk-version=24 --output=$B/dex/classes.dex $B/classes
(cd $B/dex && zip -q ../unsigned.apk classes.dex)
[ -f debug.keystore ] || keytool -genkeypair -keystore debug.keystore -storepass android -keypass android \
  -alias wizard -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Wizard Score Debug" >/dev/null 2>&1
cat > $B/Sign.java <<'J'
import com.android.apksig.ApkSigner;
import java.io.*; import java.security.*; import java.security.cert.X509Certificate; import java.util.*;
public class Sign { public static void main(String[] a) throws Exception {
  KeyStore ks = KeyStore.getInstance("PKCS12"); ks.load(new FileInputStream(a[0]), "android".toCharArray());
  PrivateKey k = (PrivateKey) ks.getKey("wizard", "android".toCharArray());
  X509Certificate c = (X509Certificate) ks.getCertificate("wizard");
  ApkSigner.SignerConfig sc = new ApkSigner.SignerConfig.Builder("wizard", k, Collections.singletonList(c)).build();
  new ApkSigner.Builder(Collections.singletonList(sc)).setInputApk(new File(a[1])).setOutputApk(new File(a[2]))
    .setMinSdkVersion(24).setV1SigningEnabled(false).setV2SigningEnabled(true).build().sign();
}}
J
javac -nowarn -cp $T/apksig-2.3.0.jar -d $B $B/Sign.java
java --add-exports java.base/sun.security.x509=ALL-UNNAMED --add-exports java.base/sun.security.pkcs=ALL-UNNAMED --add-exports java.base/sun.security.util=ALL-UNNAMED -cp $T/apksig-2.3.0.jar:$B Sign debug.keystore $B/unsigned.apk $B/WizardScore.apk
ls -la $B/WizardScore.apk
