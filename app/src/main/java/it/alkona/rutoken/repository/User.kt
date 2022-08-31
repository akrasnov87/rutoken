/*
 * Copyright (c) 2020, Aktiv-Soft JSC.
 * See the LICENSE file at the top-level directory of this distribution.
 * All Rights Reserved.
 */

package it.alkona.rutoken.repository

import com.google.gson.annotations.Expose
import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.asn1.x500.style.BCStyle
import org.bouncycastle.cert.X509CertificateHolder
import it.alkona.rutoken.database.UserEntity
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import java.io.Serializable
import java.math.BigInteger
import java.text.SimpleDateFormat
import java.util.*

data class User(
    @Expose
    val userEntity: UserEntity,
    @Expose
    val fullName: String,
    @Expose
    val position: String?,
    @Expose
    val organization: String?,
    @Expose
    val certificateExpires: String,
    @Expose
    val inn: String?,
    @Expose
    val innle: String?,
    @Expose
    val ogrn: String?,
    @Expose
    val ogrnip: String?,
    @Expose
    val algorithmId: String?,

    @Expose
    var serialNumber: BigInteger? = null,
    @Expose
    var signature: ByteArray? = null,
    @Expose
    var notBefore: Date? = null,
    @Expose
    var notAfter: Date? = null,
    @Expose
    var subjectName: X500Name? = null,
    @Expose
    var subjectPublicKeyInfo: SubjectPublicKeyInfo? = null
): Serializable

private const val INN_OID = "1.2.643.3.131.1.1"
private const val INNLE_OID = "1.2.643.100.4"
private const val OGRN_OID = "1.2.643.100.1"
private const val OGRNIP_OID = "1.2.643.100.5"

fun makeUser(
    userEntity: UserEntity
): User {
    val certificate = X509CertificateHolder(userEntity.certificateDerValue)

    check(certificate.subject.rdNs.all { !it.isMultiValued }) { "Multiple RDN values with the same type" }

    val cn = certificate.getIssuerRdnValue(BCStyle.CN)
    val surname = certificate.getIssuerRdnValue(BCStyle.SURNAME)
    val givenName = certificate.getIssuerRdnValue(BCStyle.GIVENNAME)

    val hasFullName = surname != null && givenName != null
    check(hasFullName || cn != null) { "Suitable RDNs are not found" }

    return User(
        userEntity = userEntity,
        fullName = if (hasFullName) "$surname $givenName" else cn!!,
        position = certificate.getIssuerRdnValue(BCStyle.T),
        organization = certificate.getIssuerRdnValue(BCStyle.O),
        certificateExpires = SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(certificate.notAfter),
        inn = certificate.getIssuerRdnValue(ASN1ObjectIdentifier(INN_OID)),
        innle = certificate.getIssuerRdnValue(ASN1ObjectIdentifier(INNLE_OID)),
        ogrn = certificate.getIssuerRdnValue(ASN1ObjectIdentifier(OGRN_OID)),
        ogrnip = certificate.getIssuerRdnValue(ASN1ObjectIdentifier(OGRNIP_OID)),
        algorithmId = certificate.subjectPublicKeyInfo.algorithm.algorithm.id
    )
}

private fun X509CertificateHolder.getIssuerRdnValue(type: ASN1ObjectIdentifier): String? {
    val rdn = subject.rdNs.find { it.first.type == type }
    return rdn?.first?.value?.toString()
}
