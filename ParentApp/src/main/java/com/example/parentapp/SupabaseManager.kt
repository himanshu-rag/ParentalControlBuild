package com.example.parentapp

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

object SupabaseManager {
    const val SUPABASE_URL = "https://oqbulnyfraixcerpsgwr.supabase.co"
    const val SUPABASE_KEY = "sb_publishable_8hbN6rGc48yivR8gOAG1gQ_GvmpNWYW"

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_KEY
    ) {
        install(Postgrest)
        install(Storage)
    }
}
