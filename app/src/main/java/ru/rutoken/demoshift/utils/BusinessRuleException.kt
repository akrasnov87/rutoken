package ru.rutoken.demoshift.utils

class BusinessRuleException(val case: BusinessRuleCase) : Exception(case.name)

enum class BusinessRuleCase {
    KEY_PAIR_NOT_FOUND,
    CERTIFICATE_NOT_FOUND,
    MORE_THAN_ONE_CERTIFICATE,
    WRONG_RUTOKEN,
    USER_DUPLICATES,
    FILE_UNAVAILABLE
}