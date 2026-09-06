package com.example.cloud

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class AwsS3BackupService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun testConnection(
        accessKey: String,
        secretKey: String,
        bucket: String,
        region: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (accessKey.isBlank() || secretKey.isBlank() || bucket.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Access Key, Secret Key, and Bucket Name are required"))
            }

            val testContent = "Instant Camera traveler connectivity probe: ${System.currentTimeMillis()}"
            val key = "connectivity_probe.txt"
            val body = testContent.toByteArray(Charsets.UTF_8)
            val host = "$bucket.s3.$region.amazonaws.com"
            val url = "https://$host/$key"

            val amzDate = getAmzDate()
            val dateStamp = getDateStamp()
            val payloadHash = sha256Hex(body)

            val canonicalHeaders = "host:$host\nx-amz-content-sha256:$payloadHash\nx-amz-date:$amzDate\n"
            val signedHeaders = "host;x-amz-content-sha256;x-amz-date"
            val canonicalRequest = "PUT\n/$key\n\n$canonicalHeaders\n$signedHeaders\n$payloadHash"

            val algorithm = "AWS4-HMAC-SHA256"
            val credentialScope = "$dateStamp/$region/s3/aws4_request"
            val stringToSign = "$algorithm\n$amzDate\n$credentialScope\n${sha256Hex(canonicalRequest.toByteArray(Charsets.UTF_8))}"

            val signingKey = getSignatureKey(secretKey, dateStamp, region, "s3")
            val signature = bytesToHex(hmacSha256(signingKey, stringToSign))

            val authHeader = "$algorithm Credential=$accessKey/$credentialScope, SignedHeaders=$signedHeaders, Signature=$signature"

            val request = Request.Builder()
                .url(url)
                .put(body.toRequestBody("text/plain".toMediaTypeOrNull()))
                .addHeader("Host", host)
                .addHeader("x-amz-date", amzDate)
                .addHeader("x-amz-content-sha256", payloadHash)
                .addHeader("Authorization", authHeader)
                .build()

            val response = client.newCall(request).execute()
            response.use { res ->
                if (res.isSuccessful) {
                    Result.success("Connection test passed! S3 bucket '$bucket' ($region) is reachable with write permissions.")
                } else {
                    val code = res.code
                    val errorBody = res.body?.string().orEmpty().take(300)
                    val tip = when (code) {
                        403 -> "Check IAM User permissions: 's3:PutObject' permission is required."
                        404 -> "Check Bucket Name: '$bucket' does not exist in region '$region'."
                        301 -> "Bucket might be located in a different AWS region than '$region'."
                        else -> "HTTP $code"
                    }
                    Result.failure(Exception("$tip ($code: $errorBody)"))
                }
            }
        } catch (e: Exception) {
            Log.e("AwsS3BackupService", "Test connection error", e)
            Result.failure(Exception(e.localizedMessage ?: "Failed to connect to AWS S3"))
        }
    }

    suspend fun uploadFile(
        file: File,
        mimeType: String,
        accessKey: String,
        secretKey: String,
        bucket: String,
        region: String,
        prefix: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!file.exists()) {
                return@withContext Result.failure(IllegalArgumentException("File does not exist: ${file.path}"))
            }

            val sanitizedPrefix = prefix.trim().removePrefix("/").let {
                if (it.isNotEmpty() && !it.endsWith("/")) "$it/" else it
            }
            val s3Key = "$sanitizedPrefix${file.name}"
            val host = "$bucket.s3.$region.amazonaws.com"
            val url = "https://$host/$s3Key"

            val fileBytes = file.readBytes()
            val payloadHash = sha256Hex(fileBytes)
            val amzDate = getAmzDate()
            val dateStamp = getDateStamp()

            val canonicalHeaders = "host:$host\nx-amz-content-sha256:$payloadHash\nx-amz-date:$amzDate\n"
            val signedHeaders = "host;x-amz-content-sha256;x-amz-date"
            val canonicalRequest = "PUT\n/$s3Key\n\n$canonicalHeaders\n$signedHeaders\n$payloadHash"

            val algorithm = "AWS4-HMAC-SHA256"
            val credentialScope = "$dateStamp/$region/s3/aws4_request"
            val stringToSign = "$algorithm\n$amzDate\n$credentialScope\n${sha256Hex(canonicalRequest.toByteArray(Charsets.UTF_8))}"

            val signingKey = getSignatureKey(secretKey, dateStamp, region, "s3")
            val signature = bytesToHex(hmacSha256(signingKey, stringToSign))

            val authHeader = "$algorithm Credential=$accessKey/$credentialScope, SignedHeaders=$signedHeaders, Signature=$signature"

            val request = Request.Builder()
                .url(url)
                .put(file.asRequestBody(mimeType.toMediaTypeOrNull()))
                .addHeader("Host", host)
                .addHeader("x-amz-date", amzDate)
                .addHeader("x-amz-content-sha256", payloadHash)
                .addHeader("Authorization", authHeader)
                .build()

            val response = client.newCall(request).execute()
            response.use { res ->
                if (res.isSuccessful) {
                    Result.success(url)
                } else {
                    val code = res.code
                    val errorBody = res.body?.string().orEmpty().take(200)
                    Result.failure(Exception("S3 Upload failed ($code): $errorBody"))
                }
            }
        } catch (e: Exception) {
            Log.e("AwsS3BackupService", "Upload error", e)
            Result.failure(Exception(e.localizedMessage ?: "Failed to upload file to S3"))
        }
    }

    private fun getAmzDate(): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return dateFormat.format(Date())
    }

    private fun getDateStamp(): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return dateFormat.format(Date())
    }

    private fun sha256Hex(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return bytesToHex(digest.digest(data))
    }

    private fun hmacSha256(key: ByteArray, data: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data.toByteArray(Charsets.UTF_8))
    }

    private fun getSignatureKey(key: String, dateStamp: String, regionName: String, serviceName: String): ByteArray {
        val kSecret = ("AWS4$key").toByteArray(Charsets.UTF_8)
        val kDate = hmacSha256(kSecret, dateStamp)
        val kRegion = hmacSha256(kDate, regionName)
        val kService = hmacSha256(kRegion, serviceName)
        return hmacSha256(kService, "aws4_request")
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexArray = "0123456789abcdef".toCharArray()
        val hexChars = CharArray(bytes.size * 2)
        for (j in bytes.indices) {
            val v = bytes[j].toInt() and 0xFF
            hexChars[j * 2] = hexArray[v ushr 4]
            hexChars[j * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }
}
