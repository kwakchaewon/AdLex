package com.adlex.config

import org.hibernate.dialect.H2Dialect

class TestH2Dialect : H2Dialect() {
    override fun supportsInsertReturning(): Boolean = false
}
