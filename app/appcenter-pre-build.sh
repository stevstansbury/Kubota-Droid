#!/usr/bin/env bash

# exit on first error
set -e

echo "##[section]Starting: Configure apiKey.properties"

# move up one level
cd ..

# write the apiKey.properties file
{
echo "STAGING_CLIENT_ID=\"$STAGING_CLIENT_ID\"";
echo "STAGING_CLIENT_SECRET=\"$STAGING_CLIENT_SECRET\"";
echo "STAGING_AUTH_URL=\"$STAGING_AUTH_URL\"";
echo "PROD_CLIENT_ID=\"$PROD_CLIENT_ID\"";
echo "PROD_CLIENT_SECRET=\"$PROD_CLIENT_SECRET\"";
echo "PROD_AUTH_URL=\"$PROD_CLIENT_SECRET\"";
} >>apiKey.properties

echo "##[section]Finishing: Configure apiKey.properties"
