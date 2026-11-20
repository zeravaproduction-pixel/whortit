package com.example.domain

import com.example.model.FinancialSnapshot
import com.example.model.IncomePeriod
import com.example.model.RealityCheckResult
import com.example.model.UserSettings

object FinancialCalculator {

    const val STANDARD_MONTHLY_WORK_HOURS = 160.0
    const val STANDARD_MONTHLY_DAYS = 30.0

    fun normalizeToMonthly(amount: Double, period: IncomePeriod): Double {
        if (!amount.isFinite() || amount <= 0.0) return 0.0
        return amount * period.monthlyMultiplier
    }

    fun computeSnapshot(settings: UserSettings): FinancialSnapshot {
        val monthlyIncome = normalizeToMonthly(settings.incomeAmount.toDouble(), settings.incomePeriod)
        val monthlyExpenses = normalizeToMonthly(settings.mandatoryExpenses.toDouble(), settings.incomePeriod)
        val freeFunds = monthlyIncome - monthlyExpenses
        return FinancialSnapshot(
            monthlyIncome = monthlyIncome,
            monthlyExpenses = monthlyExpenses,
            freeFunds = freeFunds
        )
    }

    fun resolveDailyAllowance(settings: UserSettings): Double {
        val custom = settings.customDailyAllowance
        if (custom != null && custom > 0L) return custom.toDouble()
        val monthlyIncome = normalizeToMonthly(settings.incomeAmount.toDouble(), settings.incomePeriod)
        return if (monthlyIncome > 0.0) monthlyIncome / STANDARD_MONTHLY_DAYS else 0.0
    }

    fun resolveHourlyWage(settings: UserSettings): Double {
        val custom = settings.customHourlyWage
        if (custom != null && custom > 0L) return custom.toDouble()
        val monthlyIncome = normalizeToMonthly(settings.incomeAmount.toDouble(), settings.incomePeriod)
        return if (monthlyIncome > 0.0) monthlyIncome / STANDARD_MONTHLY_WORK_HOURS else 0.0
    }

    fun calculateDailyEquivalent(price: Long, dailyAllowance: Double): Double? {
        if (price <= 0L || !dailyAllowance.isFinite() || dailyAllowance <= 0.0) return null
        return price.toDouble() / dailyAllowance
    }

    fun calculateWorkHours(price: Long, hourlyWage: Double): Double? {
        if (price <= 0L || !hourlyWage.isFinite() || hourlyWage <= 0.0) return null
        return price.toDouble() / hourlyWage
    }

    fun calculateRealityCheck(price: Long, settings: UserSettings): RealityCheckResult {
        val dailyAllowance = resolveDailyAllowance(settings)
        val hourlyWage = resolveHourlyWage(settings)
        val lunch = if (settings.lunchPrice > 0L) price.toDouble() / settings.lunchPrice else null
        val coffee = if (settings.coffeePrice > 0L) price.toDouble() / settings.coffeePrice else null
        val internet = if (settings.internetPrice > 0L) price.toDouble() / settings.internetPrice else null

        return RealityCheckResult(
            price = price,
            dailyAllowanceUsed = dailyAllowance,
            hourlyWageUsed = hourlyWage,
            allowanceDays = calculateDailyEquivalent(price, dailyAllowance),
            workHours = calculateWorkHours(price, hourlyWage),
            lunchPortions = lunch,
            coffeeCups = coffee,
            internetPackages = internet
        )
    }

    fun calculatePriceRatio(price: Long, freeFunds: Double): Double {
        if (price <= 0L) return 0.0
        if (!freeFunds.isFinite() || freeFunds <= 0.0) return Double.POSITIVE_INFINITY
        return price.toDouble() / freeFunds
    }
}
