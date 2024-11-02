package com.example.fitnesstracker.ui.calendar

import android.icu.util.LocaleData
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.fitnesstracker.R
import com.example.fitnesstracker.databinding.FragmentCalendarBinding
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.MonthHeaderFooterBinder
import com.kizitonwose.calendar.view.ViewContainer
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale


class CalendarFragment : Fragment() {

    private lateinit var calendarViewModel: CalendarViewModel
    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    private var selectedDate: LocalDate? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        calendarViewModel = ViewModelProvider(this).get(CalendarViewModel::class.java)
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)

        setupCalendar()
        return binding.root
    }

    private fun setupCalendar() {
        val daysOfWeek = daysOfWeek()
        val currentMonth = YearMonth.now()
        val currentDate = LocalDate.now()
        val startMonth = currentMonth.minusMonths(100)
        val endMonth = currentMonth.plusMonths(100)

        binding.calendarView.apply {
            monthHeaderBinder = object : MonthHeaderFooterBinder<MonthHeaderViewContainer> {
                override fun create(view: View) = MonthHeaderViewContainer(view)

                override fun bind(container: MonthHeaderViewContainer, data: CalendarMonth) {
                    val month = data.yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
                    val year = data.yearMonth.year.toString()

                    container.monthTextView.text = month
                    container.yearTextView.text = year

                    container.titlesContainer.children.map { it as TextView }
                        .forEachIndexed { index, textView ->
                            val title = daysOfWeek[index].getDisplayName(
                                TextStyle.SHORT,
                                Locale.getDefault()
                            ).uppercase()
                            textView.text = title

                            if (index == 0 || index == 6) {
                                textView.setTextColor(resources.getColor(android.R.color.holo_blue_dark))
                            }
                        }
                }
            }

            dayBinder = object : MonthDayBinder<DayViewContainer> {
                override fun create(view: View) = DayViewContainer(view)

                override fun bind(container: DayViewContainer, day: CalendarDay) {
                    container.day = day
                    val textView = container.textView
                    textView.text = day.date.dayOfMonth.toString()

                    when (day.position) {
                        DayPosition.MonthDate -> {
                            textView.visibility = View.VISIBLE

                            when {
                                // Selected date
                                selectedDate == day.date -> {
                                    textView.setTextColor(resources.getColor(android.R.color.black))
                                    textView.isSelected = true
                                }
                                // Today's date
                                day.date == currentDate -> {
                                    textView.setTextColor(resources.getColor(android.R.color.holo_blue_dark))
                                    textView.isSelected = false
                                }
                                // Weekend dates
                                day.date.dayOfWeek == DayOfWeek.SATURDAY ||
                                        day.date.dayOfWeek == DayOfWeek.SUNDAY -> {
                                    textView.setTextColor(resources.getColor(android.R.color.holo_blue_dark))
                                    textView.isSelected = false
                                }
                                // Regular dates
                                else -> {
                                    textView.setTextColor(resources.getColor(android.R.color.darker_gray))
                                    textView.isSelected = false
                                }
                            }
                        }
                        // Dates from another month
                        DayPosition.InDate, DayPosition.OutDate -> {
                            textView.visibility = View.INVISIBLE
                            textView.isSelected = false
                        }
                    }
                }
            }

            setup(startMonth, endMonth, daysOfWeek.first())
            scrollToMonth(currentMonth)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class DayViewContainer(view: View) : ViewContainer(view) {
        lateinit var day: CalendarDay
        val textView: TextView = view.findViewById(R.id.calendarDayText)

        init {
            view.setOnClickListener {
                if (day.position == DayPosition.MonthDate) {
                    val date = day.date
                    if (selectedDate != date) {
                        val oldDate = selectedDate
                        selectedDate = date
                        binding.calendarView.notifyDateChanged(date)
                        oldDate?.let { binding.calendarView.notifyDateChanged(it) }
                    }
                    Toast.makeText(
                        view.context,
                        date.format(DateTimeFormatter.ofPattern("dd MMM, yyyy")),
                        Toast.LENGTH_SHORT
                    ).show()                }
            }
        }
    }

    inner class MonthHeaderViewContainer(view: View) : ViewContainer(view) {
        val monthTextView: TextView = view.findViewById(R.id.monthTextView)
        val yearTextView: TextView = view.findViewById(R.id.yearTextView)
        val titlesContainer: LinearLayout = view.findViewById(R.id.titlesContainer)
    }
}
