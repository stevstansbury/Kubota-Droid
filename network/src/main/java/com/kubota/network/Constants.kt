package com.kubota.network

object Constants {
    const val BASE_URL = "https://api-kubota.azurewebsites.net/"
    //TODO(JC): Get rid of this and use something that is made at build time.
    const val STAGGING_URL = "http://aemp-staging.northcentralus.cloudapp.azure.com"
    internal const val USERNAME = "username"
    internal const val EMAIL_NAME = "email"
    internal const val PASSWORD_NAME = "password"
    internal const val REFRESH_TOKEN = "refresh_token"
    internal const val GRANT_TYPE = "grant_type"
    internal const val CLIENT_ID = "client_id"
    internal const val CLIENT_SECRET = "client_secret"
    internal const val AUTH_HEADER_KEY = "Authorization"
    //TODO(JC): Get rid of this and use something that is made at build time.
    const val LOCAL_HOST = ""
}