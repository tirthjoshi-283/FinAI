// Single source of truth for transaction categories (manual form + bill scanner).

export const DEBIT_CATEGORIES = [
    "Food",
    "Shopping",
    "Jewellery",
    "Travel",
    "Utilities",
    "Health",
    "Education",
    "Entertainment",
    "Rent",
    "Taxes",
    "Miscellaneous",
] as const;

export const CREDIT_CATEGORIES = ["Salary", "Freelance", "Investments", "Refunds", "Gifts", "Other Income"] as const;

// Guidance given to the AI so bills land in the right bucket.
export const DEBIT_CATEGORY_GUIDE = `
- Food: restaurants, hotels/dhabas (meals), cafes, Swiggy/Zomato, bakeries, groceries, supermarkets
- Shopping: clothing, shoes, accessories, electronics, gadgets, furniture, home goods, Amazon/Flipkart/Myntra orders
- Jewellery: gold, silver, diamond, jewellery stores
- Travel: flights, trains, buses, cabs (Uber/Ola), fuel/petrol, tolls, hotel room stays, travel bookings
- Utilities: electricity, water, gas cylinder, mobile/DTH recharge, internet/broadband bills
- Health: hospitals, clinics, doctors, pharmacy/medicines, diagnostics/lab tests, health insurance
- Education: school/college fees, courses, books, coaching, exam fees
- Entertainment: movies, events, concerts, OTT subscriptions, games, amusement parks
- Rent: house/shop rent, maintenance charges, PG/hostel rent
- Taxes: income tax challans, GST payments, property tax, municipal tax, any government tax/fee receipt
- Miscellaneous: anything that does not clearly fit above`.trim();
