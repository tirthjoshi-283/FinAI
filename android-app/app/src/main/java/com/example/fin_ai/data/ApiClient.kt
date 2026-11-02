package com.example.fin_ai.data

import com.example.fin_ai.BuildConfig
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Attaches the signed Firebase ID token the API verifies via firebase-admin.
 * getIdToken(false) serves the cached token and refreshes it only once expired.
 */
private class FirebaseAuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val user = FirebaseAuth.getInstance().currentUser
            ?: return chain.proceed(chain.request())

        val token = runCatching {
            Tasks.await(user.getIdToken(false), 15, TimeUnit.SECONDS).token
        }.getOrNull()

        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}

/**
 * A debug build can reach the dev server by two different routes, and which one
 * works depends on machine state nobody wants to think about while developing:
 *
 *  - "localhost" needs `adb reverse tcp:3000 tcp:3000`, which dies on every
 *    emulator restart / adb kill-server and is not recreated by pressing Run.
 *  - "10.0.2.2" is the emulator's NAT alias for the host loopback, which needs
 *    no setup but has been observed to stall on a degraded emulator network.
 *
 * So try them in order rather than making the developer pick. The first host to
 * answer is remembered, so this costs one extra connect attempt at most, and only
 * until something works. A failed host fails fast (connection refused is instant
 * when nothing is listening), which is why the connect timeout is deliberately
 * short while the read timeout stays generous for cold Atlas queries.
 */
private class HostFallbackInterceptor(private val hosts: List<String>) : Interceptor {

    @Volatile
    private var known: String? = null

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // Only rewrite hosts we are actually managing; leave anything else alone.
        if (request.url.host !in hosts) return chain.proceed(request)

        val ordered = known
            ?.let { listOf(it) + hosts.filterNot { host -> host == it } }
            ?: hosts

        var failure: IOException? = null
        for (host in ordered) {
            val attempt = request.newBuilder()
                .url(request.url.newBuilder().host(host).build())
                .build()
            try {
                val response = chain.proceed(attempt)
                known = host
                return response
            } catch (e: IOException) {
                // Host unreachable — fall through and try the next one.
                failure = e
            }
        }

        known = null
        throw failure ?: IOException("No reachable API host in $hosts")
    }
}

object ApiClient {

    /**
     * Ordered by how likely each is to be correct, not by preference: the adb
     * tunnel is checked first because when it exists it is the more reliable of
     * the two, and it fails instantly when it does not.
     */
    private val DEBUG_HOSTS = listOf("localhost", "10.0.2.2")

    val api: FinApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
            // Never let a bearer token reach logcat.
            redactHeader("Authorization")
        }

        val builder = OkHttpClient.Builder()
            .addInterceptor(FirebaseAuthInterceptor())

        // Host fallback is a local-development concern; a release build points at
        // one real host and should surface a failure rather than probe around.
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(HostFallbackInterceptor(DEBUG_HOSTS))
                .connectTimeout(8, TimeUnit.SECONDS)
        } else {
            builder.connectTimeout(30, TimeUnit.SECONDS)
        }

        val client = builder
            .addInterceptor(logging)
            // Generous: Render's free tier can take ~50 s to wake, and bill scans run an AI model.
            .readTimeout(90, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FinApi::class.java)
    }
}
