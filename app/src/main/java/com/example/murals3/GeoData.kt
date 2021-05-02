package com.example.murals3

import com.google.android.gms.maps.model.LatLng
import java.util.concurrent.TimeUnit

data class MuralPoi(val latLng: LatLng, val link: String, val title: String)

object MuralPois {
    val GEOFENCE_EXPIRATION_IN_MILLISECONDS: Long = TimeUnit.HOURS.toMillis(2)
    const val GEOFENCE_RADIUS_IN_METERS = 20f

    val data: List<MuralPoi> by lazy {
        listOf(
            MuralPoi(LatLng(48.19765620738755, 16.366615699915354), "https://www.facebook.com/viennamurals/photos/a.1741974186127461/1855760184748860/?type=3",	"1040, Margaretenstraße 6"),
            MuralPoi(LatLng(48.197355418217754, 16.366170340548862), "https://www.facebook.com/viennamurals/photos/a.1741974186127461/1743621842629362/?type=3", "1040, Margaretenstraße 7"),
            MuralPoi(LatLng(48.19766805682521, 16.364465725204408), "https://www.facebook.com/viennamurals/photos/a.1741974186127461/1972445549746989/?type=3", "1040, Mühlgasse 9"),
            MuralPoi(LatLng(48.19765834514604, 16.36150846210376), "https://www.facebook.com/viennamurals/photos/a.1741974186127461/1993058471019030/?type=3", "1060, Naschmarkt 560"),
            MuralPoi(LatLng(48.19810625315475, 16.362706061276423), "https://www.facebook.com/viennamurals/photos/a.1741974186127461/1822621708062708/?type=3", "1040, Naschmarkt / Rechte Wienzeile 21"),
            MuralPoi(LatLng(48.19792321777058, 16.36209832477071), "https://www.facebook.com/viennamurals/photos/a.1741974186127461/1773222406335972/?type=3", "1060, Naschmarkt 510"),
            MuralPoi(LatLng(48.19764927098234, 16.361707046542467), "https://www.facebook.com/viennamurals/photos/a.1741974186127461/1818428055148740/?type=3", "1060, Naschmarkt 497"),
            MuralPoi(LatLng(48.19738859313292, 16.361360486911753), "https://www.facebook.com/viennamurals/photos/a.1741974186127461/1828652050793007/?type=3", "1040, Naschmarkt / Rechte Wienzeile 29"),
            MuralPoi(LatLng(48.197810655829606, 16.360895599899305), "https://www.facebook.com/viennamurals/photos/a.1741974186127461/1827552607569618/?type=3", "1060, Linke Wienzeile 34"),
            MuralPoi(LatLng(48.199612839163244, 16.3583781742667), "https://www.facebook.com/viennamurals/photos/a.1741974186127461/1755446848113528/?type=3", "1060, Fillgradergasse 9"),
            MuralPoi(LatLng(48.200080669146104, 16.357557528108167), "https://www.facebook.com/viennamurals/photos/a.1740920956232784/1747314995593380/?type=3", "1060, Capistranstiege / Capistrangasse 1"),
            MuralPoi(LatLng(48.199336745558554, 16.357619514465163), "https://www.facebook.com/viennamurals/photos/a.1741974186127461/1965920657066145/?type=3", "1060, Fillgradergasse 15"),
            MuralPoi(LatLng(48.19931818521658, 16.356642508664198), "https://www.facebook.com/viennamurals/photos/a.1741974186127461/1985673161757561/?type=", "1060, Windmühlgasse 16"),
            MuralPoi(LatLng(48.19914384732445, 16.356438367732064), "https://www.facebook.com/viennamurals/photos/a.1741974186127461/1809960935995452/?type=", "1060, Windmühlgasse 20"),
            MuralPoi(LatLng(48.198887391620076, 16.356753317706346), "https://www.facebook.com/viennamurals/photos/a.1741974186127461/2160838484241027/?type=3", "1060, Amonstiege"),
            MuralPoi(LatLng(48.1987091552025, 16.35715598212436), "https://www.facebook.com/viennamurals/photos/a.1741974186127461/1862294697428742/?type=3", "1060, Stiegengasse 14"),
            MuralPoi(LatLng(48.19856351350978, 16.35611097967795), "https://www.facebook.com/viennamurals/photos/a.1741974186127461/2206934129631462/?type=3", "Windmühlgasse 21, 1060"),
            MuralPoi(LatLng(48.197810735557994, 16.355061829008882), "https://www.facebook.com/viennamurals/photos/a.1741974186127461/2215972275394314/?type=3", "Gumpendorfer Straße 55, 1060"),
            MuralPoi(LatLng(48.19717671272427, 16.35432521394909), "https://www.facebook.com/viennamurals/photos/a.1741974186127461/1832511643740381/?type=3", "1060, Kaunitzgasse 3-5"),
            MuralPoi(LatLng(48.19647641813919, 16.35752876976829), "https://www.facebook.com/viennamurals/photos/a.1741974186127461/2195617387429803/?type=3", "1050, Falcostiege / Rechte Wienzeile 2a"),
            MuralPoi(LatLng(48.19058047119178, 16.35972448058695), "https://www.facebook.com/viennamurals/photos/a.1741974186127461/2208324949492380/?type=3", "Schlossgasse 14, 1050"),
            MuralPoi(LatLng(48.18888841801344, 16.364957483025172), "https://www.facebook.com/viennamurals/photos/pcb.2923721774619357/2923714841286717/?type=3", "just was on PDF 1"),
            MuralPoi(LatLng(48.19277987837335, 16.37058358325985), "https://www.facebook.com/viennamurals/photos/a.1741974186127461/1865185180473027/?type=3", "1040, Favoritenstraße 30"),
            MuralPoi(LatLng(48.19172629993754, 16.370963365550217), "https://www.facebook.com/viennamurals/photos/a.1741974186127461/1747906242200922/?type=3", "1040, Favoritenstraße 36"),
            MuralPoi(LatLng(48.19123331184272, 16.372141148564108), "https://www.facebook.com/viennamurals/photos/a.1740921436232736/1740921452899401/?type=3", "1040, Theresianumgasse 33"),
            MuralPoi(LatLng(48.19159601246287, 16.37379833549732), "https://www.facebook.com/viennamurals/photos/a.1741974186127461/1801006426890903/?type=3", "1040, Theresianumgasse 22"),
            MuralPoi(LatLng(48.192629508045485, 16.3746405491292), "https://www.facebook.com/viennamurals/photos/a.1741974186127461/1813190289005850/?type=3", "1040, Argentinierstraße 37"),
            MuralPoi(LatLng(48.19330737507315, 16.37585318210831), "https://www.facebook.com/viennamurals/photos/a.1741974186127461/1993078274350383/?type=3", "1040, Schmöllerlgasse"),
            MuralPoi(LatLng(48.19444632665846, 16.37702530744593), "https://www.facebook.com/viennamurals/photos/pcb.2923721774619357/2923714841286717/?type=3", "just was on PDF 2"),
            MuralPoi(LatLng(48.19815209018646, 16.381155101465435), "https://www.facebook.com/viennamurals/photos/pcb.2923721774619357/2923714841286717/?type=3", "just was on PDF 3")
        )
    }
}