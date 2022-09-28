cat docs/openapi/pn-address-book-api-internal-v1.yaml \
    | grep -v "# NO EXTERNAL" \
    | sed -e '/# ONLY EXTERNAL/s/^#//' \
    > docs/openapi/pn-address-book-api-external-v1.yaml

cat docs/openapi/pn-user-consents-api-internal-v1.yaml \
    | grep -v "# NO EXTERNAL" \
    | sed -e '/# ONLY EXTERNAL/s/^#//' \
    > docs/openapi/pn-user-consents-api-external-v1.yaml