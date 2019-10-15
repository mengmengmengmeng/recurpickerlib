/*
 * Copyright 2019 Nicolas Maltais
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.maltaisn.recurpicker.picker

import com.maltaisn.recurpicker.Recurrence
import com.maltaisn.recurpicker.Recurrence.Period
import com.maltaisn.recurpicker.RecurrencePickerSettings
import com.maltaisn.recurpicker.dateFor
import com.maltaisn.recurpicker.format.RecurrenceFormatter
import com.nhaarman.mockitokotlin2.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.junit.MockitoJUnitRunner
import java.text.SimpleDateFormat
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


@RunWith(MockitoJUnitRunner::class)
internal class RecurrencePickerPresenterTest {

    private val presenter = RecurrencePickerPresenter()
    private val settings = RecurrencePickerSettings {
        formatter = RecurrenceFormatter(SimpleDateFormat("yyyy-MM-dd"))
        defaultPickerRecurrence = Recurrence(Period.DAILY) {
            frequency = 3
            endDate = dateFor("2019-12-31")
        }
    }

    private val view: RecurrencePickerContract.View = mock {
        on { settings } doReturn settings
        on { startDate } doReturn dateFor("2019-10-31")
        on { endDateText } doReturn "p|s"
        on { getEndCountTextFor(anyInt()) } doReturn "p|s"
    }

    @Before
    fun setUp() {
        presenter.attach(view, null)
        clearInvocations(view)
    }

    @Test
    fun onCancel() {
        presenter.onCancel()
        verify(view).setCancelResult()
        verify(view).exit()
    }

    @Test
    fun onConfirm() {
        presenter.onConfirm()
        verify(view).setConfirmResult(settings.defaultPickerRecurrence)
        verify(view).exit()
    }

    @Test
    fun onFrequencyChanged() {
        presenter.onFrequencyChanged("12")
        verify(view).setPeriodItems(12)
        verify(view).setSelectedPeriodItem(0)
        assertEquals(12, createRecurrence().frequency)
    }

    @Test
    fun onFrequencyChanged_notChanged() {
        presenter.onFrequencyChanged("3")
        verify(view, never()).setPeriodItems(anyInt())
    }

    @Test
    fun onFrequencyChanged_invalid() {
        presenter.onFrequencyChanged("foo")
        verify(view).setPeriodItems(1)
        verify(view).setSelectedPeriodItem(0)
    }

    @Test
    fun onPeriodItemSelected_daily() {
        presenter.onPeriodItemSelected(0)
        verify(view).setWeekBtnsShown(false)
        verify(view).setMonthlySettingShown(false)
        assertEquals(Period.DAILY, createRecurrence().period)
    }

    @Test
    fun onPeriodItemSelected_weekly() {
        presenter.onPeriodItemSelected(1)
        verify(view).setWeekBtnsShown(true)
        verify(view).setMonthlySettingShown(false)
        assertEquals(Period.WEEKLY, createRecurrence().period)
    }

    @Test
    fun onPeriodItemSelected_monthly() {
        presenter.onPeriodItemSelected(2)
        verify(view).setWeekBtnsShown(false)
        verify(view).setMonthlySettingShown(true)
        assertEquals(Period.MONTHLY, createRecurrence().period)
    }

    @Test
    fun onPeriodItemSelected_yearly() {
        presenter.onPeriodItemSelected(3)
        verify(view).setWeekBtnsShown(false)
        verify(view).setMonthlySettingShown(false)
        assertEquals(Period.YEARLY, createRecurrence().period)
    }

    @Test
    fun onWeekBtnsChecked() {
        presenter.onPeriodItemSelected(1)  // Set period to weekly
        presenter.onWeekBtnChecked(Calendar.TUESDAY, true)
        presenter.onWeekBtnChecked(Calendar.THURSDAY, false)
        presenter.onWeekBtnChecked(Calendar.FRIDAY, true)

        val r = createRecurrence()
        assertTrue(r.isRecurringOnDaysOfWeek(Recurrence.TUESDAY))
        assertFalse(r.isRecurringOnDaysOfWeek(Recurrence.THURSDAY))
        assertTrue(r.isRecurringOnDaysOfWeek(Recurrence.FRIDAY))
    }

    @Test
    fun onMonthlySettingSelected_sameDay() {
        presenter.onPeriodItemSelected(2)  // Set period to monthly
        presenter.onMonthlySettingItemSelected(0)
        assertEquals(0, createRecurrence().byMonthDay)
    }

    @Test
    fun onMonthlySettingSelected_sameWeek() {
        presenter.onPeriodItemSelected(2)  // Set period to monthly
        presenter.onMonthlySettingItemSelected(1)

        val r = createRecurrence()
        assertEquals(Calendar.THURSDAY, r.dayOfWeekInMonth)
        assertEquals(-1, r.weekInMonth)
    }

    @Test
    fun onMonthlySettingSelected_lastDay() {
        presenter.onPeriodItemSelected(2)  // Set period to monthly
        presenter.onMonthlySettingItemSelected(2)
        assertEquals(-1, createRecurrence().byMonthDay)
    }

    @Test
    fun onEndNeverClicked() {
        presenter.onEndNeverClicked()
        verify(view).setEndNeverChecked(true)
        assertEquals(Recurrence.EndType.NEVER, createRecurrence().endType)
    }

    @Test
    fun onEndDateClicked() {
        presenter.onEndNeverClicked()  // Set to never, because already set to end by date.
        presenter.onEndDateClicked()
        verify(view).setEndDateChecked(true)
        verify(view).setEndDateViewEnabled(true)
        assertEquals(Recurrence.EndType.BY_DATE, createRecurrence().endType)
    }

    @Test
    fun onEndCountClicked() {
        presenter.onEndCountClicked()
        verify(view).setEndCountChecked(true)
        verify(view).setEndCountViewEnabled(true)
        assertEquals(Recurrence.EndType.BY_COUNT, createRecurrence().endType)
    }

    @Test
    fun onEndDateInputClicked() {
        presenter.onEndDateInputClicked()
        verify(view).showEndDateDialog(settings.defaultPickerRecurrence.endDate, view.startDate)
    }

    @Test
    fun onEndDateEntered() {
        val date = dateFor("2033-01-01")
        presenter.onEndDateEntered(date)
        verify(view).setEndDateView("2033-01-01")
        assertEquals(date, createRecurrence().endDate)
    }

    @Test
    fun onEndCountChanged() {
        presenter.onEndCountClicked()
        presenter.onEndCountChanged("666")
        verify(view).setEndCountLabels(any(), any())
        assertEquals(666, createRecurrence().endCount)
    }

    @Test
    fun onEndCountChanged_notChanged() {
        presenter.onEndCountClicked()
        presenter.onEndCountChanged("1")
        verify(view, never()).setEndCountLabels(any(), any())
    }

    @Test
    fun onEndCountChanged_invalid() {
        presenter.onEndCountClicked()
        presenter.onEndCountChanged("12")  // Changed it because invalid sets to 1 and it's already 1.
        clearInvocations(view)
        presenter.onEndCountChanged("foo")
        verify(view).setEndCountLabels(any(), any())
    }

    private fun createRecurrence(): Recurrence {
        // Create the recurrence, capture it, and return it.
        presenter.onConfirm()
        argumentCaptor<Recurrence>().apply {
            verify(view).setConfirmResult(capture())
            return allValues.first()
        }
    }

}