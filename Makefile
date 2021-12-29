
app=Altimeter

sign:
	jarsigner -sigalg SHA1withRSA -digestalg SHA1 -keystore ~/.android/keystore ${app}.apk falk-new
	jarsigner -verify -verbose -keystore ~/.android/keystore ${app}.apk
	zipalign -v 4 ${app}.apk tmp.apk
	mv tmp.apk ${app}.apk
