package com.example.fitnesstracker.ui.calendar

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CalendarView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.example.fitnesstracker.R
import com.example.fitnesstracker.databinding.FragmentCalendarBinding
import com.example.fitnesstracker.models.DailySteps
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.MonthHeaderFooterBinder
import com.kizitonwose.calendar.view.ViewContainer
import com.mikhaellopez.circularprogressbar.CircularProgressBar

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale


class CalendarFragment : Fragment() {

    private lateinit var calendarViewModel: CalendarViewModel
    private val mAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!
    private val stepsData = mutableMapOf<LocalDate, Int>()

    private var selectedDate: LocalDate? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        calendarViewModel = ViewModelProvider(this).get(CalendarViewModel::class.java)
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)

        setupCalendar()

        //Fetch data for each day in the calendar
        fetchStepData()

        // Set default data for today's date
        setDefaultTodayData()

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
                    var progressBar = container.progressBar


                    when (day.position) {
                        DayPosition.MonthDate -> {
                            textView.visibility = View.VISIBLE

                            val stepsForDay = stepsData[day.date]
                            if (stepsForDay != null) {
                                progressBar.setProgressWithAnimation(stepsForDay.toFloat())
                            } else {
                                progressBar.setProgressWithAnimation(0f)
                            }
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

    private fun fetchStepData() {
        val currentUser = mAuth.currentUser?.displayName ?: return
        db.collection("dailySteps")
            .whereEqualTo("displayname", currentUser)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val dailySteps = document.toObject(DailySteps::class.java)
                    dailySteps.date?.let { date ->
                        val localDate = LocalDate.parse(date)
                        stepsData[localDate] = dailySteps.steps.toIntOrNull() ?: 0
                    }
                }
                binding.calendarView.notifyCalendarChanged()
            }
            .addOnFailureListener { e ->
                Log.e("CalendarFragment", "Error fetching steps data", e)
            }
    }

    private fun setDefaultTodayData() {
        val today = LocalDate.now()
        selectedDate = today

        val stepsToday = stepsData[today] ?: 0
        val caloriesBurnedToday = (stepsToday * 0.04).toFloat()
        val distanceWalkedToday = (stepsToday * 80) / 100000f

        binding.apply {
            calendarSelectedDate.text = today.format(DateTimeFormatter.ofPattern("dd MMM, yyyy"))
            calendarSelectedSteps.text = "$stepsToday steps"
            calendarDistance.text = String.format("%.2f km", distanceWalkedToday)
            calendarCalories.text = String.format("%.2f kcal", caloriesBurnedToday)

            calendarSelectedStepsProgressBar.setProgressWithAnimation(stepsToday.toFloat())
            calendarCaloriesProgressBar.setProgressWithAnimation(caloriesBurnedToday)
            calendarKmProgressBar.setProgressWithAnimation(distanceWalkedToday)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class DayViewContainer(view: View) : ViewContainer(view) {
        lateinit var day: CalendarDay
        val textView: TextView = view.findViewById(R.id.calendarDayText)
        val progressBar: CircularProgressBar = view.findViewById(R.id.calendarStepsProgressBar)

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
                    val steps_init = stepsData[day.date] ?: 0
                    val caloriesBurned_init = (steps_init.toFloat()*0.04).toFloat()
                    val distanceWalked_init = (steps_init.toFloat()*80)/100000

                    binding.apply {
                        calendarSelectedDate.text = date.format(DateTimeFormatter.ofPattern("dd MMM, yyyy"))
                        calendarSelectedSteps.text = steps_init.toString() + " steps"
                        calendarDistance.text = distanceWalked_init.toString() + " km"
                        calendarCalories.text = caloriesBurned_init.toString() + " kcal"

                        calendarSelectedStepsProgressBar.apply {
                            setProgressWithAnimation(steps_init.toFloat())
                        }
                        calendarCaloriesProgressBar.apply {
                            setProgressWithAnimation(caloriesBurned_init)
                        }
                        calendarKmProgressBar.apply {
                            setProgressWithAnimation(distanceWalked_init)
                        }
                    }

                }
            }
        }
    }


    inner class MonthHeaderViewContainer(view: View) : ViewContainer(view) {
        val monthTextView: TextView = view.findViewById(R.id.monthTextView)
        val yearTextView: TextView = view.findViewById(R.id.yearTextView)
        val titlesContainer: LinearLayout = view.findViewById(R.id.titlesContainer)
    }
}
