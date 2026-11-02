package com.example.fin_ai.data

/** Mirrors lib/categories.ts on the web app so both clients file entries identically. */
object Categories {
    val debit = listOf(
        "Food", "Shopping", "Jewellery", "Travel", "Utilities", "Health",
        "Education", "Entertainment", "Rent", "Taxes", "Miscellaneous",
    )
    val credit = listOf("Salary", "Freelance", "Investments", "Refunds", "Gifts", "Other Income")
}
