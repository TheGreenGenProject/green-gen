package org.greengen.core

import org.greengen.core.Coordinate.LatLong


sealed trait Location
case class OnLine(url: Url) extends Location
case class GeoLocation(coordinates: LatLong) extends Location
case class Address(
  address: Option[String],
  zipCode: Option[String],
  country: Country,
) extends Location


sealed trait Country
object Country {
  // Everywhere !
  case object World extends Country
  // All countries
  case object Afghanistan extends Country
  case object Akrotiri extends Country
  case object Åland extends Country
  case object Albania extends Country
  case object Algeria extends Country
  case object American extends Country
  case object Andorra extends Country
  case object Angola extends Country
  case object Anguilla extends Country
  case object Antarctica extends Country
//  case object Antigua extends Country
//  case object Argentina extends Country
//  case object Armenia extends Country
//  case object Aruba extends Country
//  case object Australia extends Country
//  case object Austria extends Country
//  case object Azerbaijan extends Country
//  case object Bahrain extends Country
//  case object Bangladesh extends Country
//  case object Barbados extends Country
//  case object Belarus extends Country
//  case object Belgium extends Country
//  case object Belize extends Country
//  case object Benin extends Country
//  case object Bermuda extends Country
//  case object Bhutan extends Country
//  case object Bolivia extends Country
//  case object Bonaire extends Country
//  case object Bosnia extends Country
//  case object Botswana extends Country
//  case object BouvetIsland extends Country
//  case object Brazil extends Country
//  case object Brunei extends Country
//  case object Bulgaria extends Country
//  case object BurkinaFaso extends Country
//  case object Myanmar extends Country
//  case object Burundi extends Country
//  case object Cape extends Country
//  case object Cambodia extends Country
//  case object Cameroon extends Country
//  case object Canada extends Country
//  case object Cayman extends Country
//  case object Central extends Country
//  case object Chad extends Country
//  case object Chile extends Country
//  case object China extends Country
//  case object Christmas extends Country
//  case object Cocos extends Country
//  case object Colombia extends Country
//  case object Comoros extends Country
//  case object Cook extends Country
//  case object Costa extends Country
//  case object Croatia extends Country
//  case object Cuba extends Country
//  case object Curacao extends Country
//  case object Cyprus extends Country
//  case object Czech extends Country
//  case object Democratic extends Country
//  case object Denmark extends Country
//  case object Djibouti extends Country
//  case object Dominica extends Country
//  case object Dominican extends Country
//  case object East extends Country
//  case object Ecuador extends Country
//  case object Egypt extends Country
//  case object ElSalvador extends Country
//  case object England extends Country
//  case object Equatorial extends Country
//  case object Eritrea extends Country
//  case object Estonia extends Country
//  case object Eswatini extends Country
//  case object Ethiopia extends Country
//  case object Falkland extends Country
//  case object FaroeIslands extends Country
//  case object Fiji extends Country
//  case object Finland extends Country
//  case object France extends Country
//  case object Gabon extends Country
//  case object Georgia extends Country
//  case object Germany extends Country
//  case object Ghana extends Country
//  case object Gibraltar extends Country
//  case object Greece extends Country
//  case object Greenland extends Country
//  case object Grenada extends Country
//  case object Guadeloupe extends Country
//  case object Guam extends Country
//  case object Guatemala extends Country
//  case object Bailiwick extends Country
//  case object Guinea extends Country
//  case object GuineaBissau extends Country
//  case object Guyana extends Country
//  case object Haiti extends Country
//  case object Heard extends Country
//  case object Holy extends Country
//  case object Honduras extends Country
//  case object HongKong extends Country
//  case object Hungary extends Country
//  case object Iceland extends Country
//  case object India extends Country
//  case object Indonesia extends Country
//  case object Iran extends Country
//  case object Iraq extends Country
//  case object Republic extends Country
//  case object Israel extends Country
//  case object Italy extends Country
//  case object Ivory extends Country
//  case object Jamaica extends Country
//  case object Jan extends Country
//  case object Japan extends Country
//  case object Jersey extends Country
//  case object Jordan extends Country
//  case object Kazakhstan extends Country
//  case object Kenya extends Country
//  case object Kiribati extends Country
//  case object South extends Country
//  case object Kuwait extends Country
//  case object Kyrgyzstan extends Country
//  case object Laos extends Country
//  case object Latvia extends Country
//  case object Lebanon extends Country
//  case object Lesotho extends Country
//  case object Liberia extends Country
//  case object Libya extends Country
//  case object Liechtenstein extends Country
//  case object Lithuania extends Country
//  case object Luxembourg extends Country
//  case object Madagascar extends Country
//  case object Malawi extends Country
//  case object Malaysia extends Country
//  case object Maldives extends Country
//  case object Mali extends Country
//  case object Malta extends Country
//  case object Marshall extends Country
//  case object Martinique extends Country
//  case object Mauritania extends Country
//  case object Mauritius extends Country
//  case object Mayotte extends Country
//  case object Mexico extends Country
//  case object Federated extends Country
//  case object Moldova extends Country
//  case object Monaco extends Country
//  case object Mongolia extends Country
//  case object Montenegro extends Country
//  case object Montserrat extends Country
//  case object Morocco extends Country
//  case object Mozambique extends Country
//  case object Namibia extends Country
//  case object Nauru extends Country
//  case object Nepal extends Country
//  case object Netherlands extends Country
//  case object Nicaragua extends Country
//  case object Niger extends Country
//  case object Nigeria extends Country
//  case object Niue extends Country
//  case object Norfolk extends Country
//  case object Norway extends Country
//  case object Oman extends Country
//  case object Pakistan extends Country
//  case object Palau extends Country
//  case object Panama extends Country
//  case object Papua extends Country
//  case object Paraguay extends Country
//  case object Peru extends Country
//  case object Philippines extends Country
//  case object Pitcairn extends Country
//  case object Poland extends Country
//  case object Portugal extends Country
//  case object Qatar extends Country
//  case object Reunion extends Country
//  case object Romania extends Country
//  case object Russia extends Country
//  case object Rwanda extends Country
//  case object Saba extends Country
//  case object Sahrawi extends Country
//  case object Ascension extends Country
//  case object Tristan extends Country
//  case object Collectivity extends Country
//  case object Samoa extends Country
//  case object SaudiArabia extends Country
//  case object Scotland extends Country
//  case object Senegal extends Country
//  case object Serbia extends Country
//  case object Seychelles extends Country
//  case object Sierra extends Country
//  case object Singapore extends Country
//  case object Sint extends Country
//  case object Slovakia extends Country
//  case object Slovenia extends Country
//  case object Solomon extends Country
//  case object Somalia extends Country
//  case object Spain extends Country
//  case object Sri extends Country
//  case object Sudan extends Country
//  case object Suriname extends Country
//  case object Svalbard extends Country
//  case object Sweden extends Country
//  case object Switzerland extends Country
//  case object Syria extends Country
//  case object Taiwan extends Country
//  case object Tajikistan extends Country
//  case object Tanzania extends Country
//  case object Thailand extends Country
//  case object Togo extends Country
//  case object Tokelau extends Country
//  case object Tonga extends Country
//  case object Trinidad extends Country
//  case object Tunisia extends Country
//  case object Turkey extends Country
//  case object Turkmenistan extends Country
//  case object Turks extends Country
//  case object Tuvalu extends Country
//  case object Uganda extends Country
//  case object Ukraine extends Country
//  case object UnitedArabEmirates extends Country
  case object UnitedKingdom extends Country
//  case object UnitedStates extends Country
//  case object Uruguay extends Country
//  case object Uzbekistan extends Country
//  case object Vanuatu extends Country
//  case object Vatican extends Country
//  case object Venezuela extends Country
//  case object Vietnam extends Country
//  case object Wales extends Country
//  case object Wallis extends Country
//  case object Western extends Country
//  case object Yemen extends Country
//  case object Zambia extends Country
//  case object Zimbabwe extends Country
}