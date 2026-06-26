import { createClient } from '@supabase/supabase-js'

const SUPABASE_URL = 'https://oqbulnyfraixcerpsgwr.supabase.co'
const SUPABASE_KEY = 'sb_publishable_8hbN6rGc48yivR8gOAG1gQ_GvmpNWYW'

export const supabase = createClient(SUPABASE_URL, SUPABASE_KEY)
