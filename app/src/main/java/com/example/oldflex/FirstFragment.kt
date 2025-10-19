package com.example.oldflex

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.oldflex.databinding.FragmentFirstBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class FirstFragment : Fragment() {
    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var offersRecyclerView: RecyclerView
    private lateinit var offersAdapter: OffersAdapter
    private var offersList = mutableListOf<Offer>()
    private var isscheduled = false
    private var previousOffersCount = 0
    private var selectedDate: Calendar = Calendar.getInstance()
    private val dateOffersMap = mutableMapOf<String, List<Offer>>() // Cache offers by date

    private val removedOfferIds = mutableSetOf<String>()
    private val refreshTimeoutHandler = Handler(Looper.getMainLooper())
    private val refreshTimeoutRunnable = Runnable {
        if (isAdded && _binding != null && swipeRefreshLayout.isRefreshing) {
            swipeRefreshLayout.isRefreshing = false
        }
    }
    private var refreshJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        parentFragmentManager.setFragmentResultListener("delivery_scheduled", viewLifecycleOwner) { requestKey, bundle ->
            if (isAdded && _binding != null) {
                val location = bundle.getString("scheduled_location", "")
                isscheduled = true
                val time = bundle.getString("scheduled_time", "")
                val price = bundle.getString("scheduled_price", "")
                showScheduledPopup(location, time, price)
            }
        }

        initializeViews()
        setupRecyclerView()
        setupRefreshLayout()
        setupUpdateButton()
        setCurrentDate()
        populateDateContainer()
        // Initial load
        updateSelectedDateText() // show today by default
        refreshOffersForSelectedDate()

    }

    private fun initializeViews() {
        if (!isAdded || _binding == null) return
        swipeRefreshLayout = binding.swipeRefreshLayout
        offersRecyclerView = binding.offersRecyclerView
    }

    private fun setupRecyclerView() {
        if (!isAdded || _binding == null) return
        offersAdapter = OffersAdapter(offersList) { offer ->
            if (isAdded && _binding != null) {
                removedOfferIds.add(offer.id)
                navigateToDeliveryOffer(offer)
            }
        }
        offersRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        offersRecyclerView.adapter = offersAdapter
    }

    private fun setupRefreshLayout() {
        if (!isAdded || _binding == null) return
        swipeRefreshLayout.setOnRefreshListener {
            refreshOffers()
        }

        swipeRefreshLayout.setColorSchemeResources(
            android.R.color.holo_orange_dark,
            android.R.color.holo_orange_light
        )

        swipeRefreshLayout.setDistanceToTriggerSync(80)

        swipeRefreshLayout.setProgressViewOffset(false, 0, 150)
    }
    private fun populateDateContainer() {
        if (!isAdded || _binding == null) return

        val dateContainer = binding.root.findViewById<LinearLayout>(R.id.date_container)
        dateContainer.removeAllViews()

        val today = Calendar.getInstance()
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val dateFormat = SimpleDateFormat("d", Locale.getDefault())

        repeat(7) { index ->
            val dayView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_date, dateContainer, false)

            // Clone a new calendar for each date
            val thisDate = today.clone() as Calendar
            thisDate.add(Calendar.DAY_OF_YEAR, index)

            val dayText = dayView.findViewById<TextView>(R.id.dayText)
            val dateText = dayView.findViewById<TextView>(R.id.dateText)

            dayText.text = dayFormat.format(thisDate.time)
            dateText.text = dateFormat.format(thisDate.time)

            // Highlight today (index == 0)
            if (index == 0) highlightSelectedDate(dayView)

            // Set click listener for this specific date
            dayView.setOnClickListener {
                selectedDate.time = thisDate.time
                updateSelectedDateText()
                highlightSelectedDate(dayView)
                refreshOffersForSelectedDate()
            }

            dateContainer.addView(dayView)
        }
    }

    private fun highlightSelectedDate(selectedView: View) {
        val dateContainer = binding.root.findViewById<LinearLayout>(R.id.date_container)

        for (i in 0 until dateContainer.childCount) {
            val child = dateContainer.getChildAt(i)
            val underline = child.findViewById<View>(R.id.underline)
            underline.visibility = View.GONE
        }

        val selectedUnderline = selectedView.findViewById<View>(R.id.underline)
        selectedUnderline.visibility = View.VISIBLE
    }

    private fun updateSelectedDateText() {
        val dateFormat = SimpleDateFormat("EEEE, M/d", Locale.ENGLISH)
        binding.dateText.text = dateFormat.format(selectedDate.time)
    }

    private fun refreshOffersForSelectedDate() {
        val dateKey = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(selectedDate.time)

        // Try to use cached offers first
        val cachedOffers = dateOffersMap[dateKey]
        if (cachedOffers != null) {
            offersList.clear()
            offersList.addAll(cachedOffers)
            offersAdapter.notifyDataSetChanged()
            updateOffersCount()
            checkEmptyState()
            return
        }

        // Otherwise, generate new offers for that date
        swipeRefreshLayout.isRefreshing = true

        refreshJob?.cancel()
        refreshJob = CoroutineScope(Dispatchers.Default).launch {
            delay(200) // simulate network delay
            val newOffers = generateNewOffers()
            dateOffersMap[dateKey] = newOffers // cache it

            withContext(Dispatchers.Main) {
                offersList.clear()
                offersList.addAll(newOffers)
                offersAdapter.notifyDataSetChanged()
                updateOffersCount()
                swipeRefreshLayout.isRefreshing = false
                checkEmptyState()
            }
        }
    }

    private fun setupUpdateButton() {
        if (!isAdded || _binding == null) return
        binding.updateButton.setOnClickListener {
            if (!isAdded || _binding == null) return@setOnClickListener

            // Prevent rapid clicks
            binding.updateButton.isEnabled = false
            binding.updateButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.button_pressed_color))
            swipeRefreshLayout.isRefreshing = true
            refreshOffers()

            Handler(Looper.getMainLooper()).postDelayed({
                if (isAdded && _binding != null) {
                    binding.updateButton.setBackgroundColor(
                        ContextCompat.getColor(requireContext(), R.color.button_default_color)
                    )
                    binding.updateButton.isEnabled = true
                }
            }, 1000)
        }
    }

    private fun setCurrentDate() {
        if (!isAdded || _binding == null) return
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 1) // Move to tomorrow

        val dateFormat = SimpleDateFormat("EEEE, M/d", Locale.ENGLISH)
        binding.dateText.text = dateFormat.format(calendar.time)
    }

    private fun checkEmptyState() {
        if (!isAdded || _binding == null) return
        view?.post {
            if (offersList.isEmpty()) {
                binding.noOffersContainer.visibility = View.VISIBLE
                binding.offersRecyclerView.visibility = View.GONE
                binding.offersText.visibility = View.GONE
                binding.dateText.visibility = View.GONE
            } else {
                binding.noOffersContainer.visibility = View.GONE
                binding.offersRecyclerView.visibility = View.VISIBLE
                binding.offersText.visibility = View.VISIBLE
                binding.dateText.visibility = View.VISIBLE
            }
        }
    }

    private fun navigateToDeliveryOffer(offer: Offer) {
        if (!isAdded) return
        val bundle = Bundle().apply {
            putString("location", offer.location)
            putString("timeRange", "${offer.startTime} - ${offer.endTime}")
            putString("duration", offer.duration)
            putString("price", offer.price)
        }
        findNavController().navigate(
            R.id.action_FirstFragment_to_DeliveryOfferFragment,
            bundle
        )
    }

    private fun showScheduledPopup(location: String, time: String, price: String) {
        if (!isAdded || _binding == null) return

        view?.post {
            val popupView = LayoutInflater.from(requireContext())
                .inflate(R.layout.notification_popup, binding.root as ViewGroup, false)

            popupView.findViewById<ImageView>(R.id.iconClose).setOnClickListener {
                if (isAdded && _binding != null) {
                    animatePopupDismiss(popupView)
                }
            }

            popupView.setOnClickListener {
                if (isAdded) {
                    val intent = Intent(requireContext(), CalendarActivity::class.java)
                    startActivity(intent)
                    animatePopupDismiss(popupView)
                }
            }

            val toolbar = binding.root.findViewById<Toolbar>(R.id.toolbar)
            val toolbarHeight = toolbar.height

            (binding.root as ViewGroup).addView(popupView)
            popupView.translationY = -popupView.height.toFloat() - toolbarHeight

            popupView.animate()
                .translationY(0f)
                .setDuration(300)
                .setInterpolator(DecelerateInterpolator())
                .start()

            Handler(Looper.getMainLooper()).postDelayed({
                if (isAdded && _binding != null) {
                    animatePopupDismiss(popupView)
                }
            }, 5000)
        }
    }

    private fun animatePopupDismiss(popupView: View) {
        if (!isAdded || _binding == null) return

        view?.post {
            val toolbar = binding.root.findViewById<Toolbar>(R.id.toolbar)
            val toolbarHeight = toolbar.height

            popupView.animate()
                .translationY(-popupView.height.toFloat() - toolbarHeight)
                .setDuration(300)
                .setInterpolator(AccelerateInterpolator())
                .withEndAction {
                    if (isAdded && _binding != null) {
                        (binding.root as ViewGroup).removeView(popupView)
                    }
                }
                .start()
        }
    }

    private fun refreshOffers() {
        if (!isAdded || _binding == null) return

        refreshJob?.cancel()

        try {
            view?.post {
                if (isAdded && _binding != null) {
                    swipeRefreshLayout.isRefreshing = true
                }
            }

            previousOffersCount = offersList.size
            refreshTimeoutHandler.postDelayed(refreshTimeoutRunnable, 10000)

            refreshJob = CoroutineScope(Dispatchers.Default).launch {
                val currentOffers = offersList.toList()
                val randomDelay = Random.nextLong(100, 250)
                delay(randomDelay)

                if (!isAdded) return@launch

                val newOffers = generateNewOffers().filterNot { offer ->
                    removedOfferIds.contains(offer.id)
                }

                withContext(Dispatchers.Main) {
                    if (!isAdded || _binding == null) return@withContext

                    refreshTimeoutHandler.removeCallbacks(refreshTimeoutRunnable)

                    // ✅ Update only the selected date’s cache
                    val dateKey = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(selectedDate.time)
                    dateOffersMap[dateKey] = newOffers

                    updateOffersList(newOffers, currentOffers)
                    updateOffersCount()
                    swipeRefreshLayout.isRefreshing = false
                    checkEmptyState()
                }
            }
        } catch (e: Exception) {
            refreshTimeoutHandler.removeCallbacks(refreshTimeoutRunnable)
            view?.post {
                if (isAdded && _binding != null) {
                    swipeRefreshLayout.isRefreshing = false
                    checkEmptyState()
                }
            }
        }
    }


    private fun updateOffersList(newOffers: List<Offer>, currentOffers: List<Offer>) {
        if (!isAdded || _binding == null) return

        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = currentOffers.size
            override fun getNewListSize(): Int = newOffers.size

            override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean {
                return currentOffers[oldPos].id == newOffers[newPos].id
            }

            override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean {
                return currentOffers[oldPos] == newOffers[newPos]
            }
        })

        offersList.clear()
        offersList.addAll(newOffers)

        view?.post {
            if (isAdded && _binding != null) {
                diffResult.dispatchUpdatesTo(offersAdapter)
                if (newOffers.size != previousOffersCount) {
                    blinkOffersText()
                }
                checkEmptyState()
            }
        }
    }

    private fun generateNewOffers(): List<Offer> {
        val shouldGenerateNew = (1..100).random() <= 30
        if (!shouldGenerateNew) return offersList.toList()

        val newOfferCount = if (isscheduled) (1..1).random() else (1..2).random()
        val newOffers = mutableListOf<Offer>()
        var attempts = 0

        while (newOffers.size < newOfferCount && attempts < 100) {
            attempts++
            val offer = generateRandomOffer()
            if (!removedOfferIds.contains(offer.id)) {
                newOffers.add(offer)
            }
        }

        return newOffers.sortedBy { it.startTime }
    }

    private fun blinkOffersText() {
        if (!isAdded || _binding == null) return

        view?.post {
            _binding?.offersText?.visibility = View.INVISIBLE
            view?.postDelayed({
                if (isAdded && _binding != null) {
                    _binding?.offersText?.visibility = View.VISIBLE
                }
            }, 200)
        }
    }

    private fun updateOffersCount() {
        if (!isAdded || _binding == null) return
        binding.offersText.text = "${offersList.size} of ${offersList.size} offers"
    }

    // Data class for easier structuring
    data class Shift(val durationMinutes: Int, val priceRange: Pair<Float, Float>, val weight: Int)

    // Weighted shift list (based on your probability distribution)
    val shifts = listOf(     // 1 hr 30 min    // 2 hr
        Shift(150, Pair(50f, 80f), 2),   // 2 hr 30 min
        Shift(180, Pair(60f, 90f), 5),   // 3 hr
        Shift(210, Pair(90f, 120f), 10),  // 3 hr 30 min
        Shift(240, Pair(90f, 120f), 40),  // 4 hr
        Shift(270, Pair(120f, 170f), 35)  // 4 hr 30 min
    )

    // Weighted selection function
    fun selectWeightedShift(): Shift {
        val totalWeight = shifts.sumOf { it.weight }
        val randomValue = Random.nextInt(totalWeight)
        var cumulative = 0
        for (shift in shifts) {
            cumulative += shift.weight
            if (randomValue < cumulative) return shift
        }
        return shifts.last() // fallback
    }

    fun randomFloatBetween(min: Float, max: Float): Float {
        val randomValue = min + Random.nextFloat() * (max - min)
        return randomValue.roundToInt().toFloat()  // Rounds to nearest whole number
    }
    private fun generateRandomOffer(): Offer {
        val locations = listOf(
            "Portland, OR (PDX5) - Amazon.com",
            "Portland, OR (PDX6) - Amazon.com",
            "Eugene, OR (EUG5) - Amazon.com",
            "Salem, OR (PDX7) - Amazon.com",
            "Troutdale, OR (PDX9) - Amazon.com"
        )

        val location = locations.random()
        val selectedShift = selectWeightedShift()
        val durationMinutes = selectedShift.durationMinutes

        val now = Calendar.getInstance()
        val today = Calendar.getInstance()

        val minStartTime = today.clone() as Calendar
        minStartTime.set(Calendar.HOUR_OF_DAY, 6)
        minStartTime.set(Calendar.MINUTE, 0)

        val maxStartTime = today.clone() as Calendar
        maxStartTime.set(Calendar.HOUR_OF_DAY, 21)
        maxStartTime.set(Calendar.MINUTE, 0)
        maxStartTime.add(Calendar.MINUTE, -durationMinutes)

        val randomTimeMillis = randomLongBetween(
            minStartTime.timeInMillis.coerceAtLeast(now.timeInMillis),
            maxStartTime.timeInMillis
        )

        val randomStartTime = Calendar.getInstance().apply {
            timeInMillis = randomTimeMillis

            val minute = get(Calendar.MINUTE)
            when {
                minute < 8 -> set(Calendar.MINUTE, 0)
                minute < 23 -> set(Calendar.MINUTE, 15)
                minute < 38 -> set(Calendar.MINUTE, 30)
                minute < 53 -> set(Calendar.MINUTE, 45)
                else -> {
                    set(Calendar.MINUTE, 0)
                    add(Calendar.HOUR_OF_DAY, 1)
                }
            }
        }

        if (randomStartTime.before(now)) {
            randomStartTime.time = now.time
            val minute = randomStartTime.get(Calendar.MINUTE)
            when {
                minute < 30 -> randomStartTime.set(Calendar.MINUTE, 30)
                else -> {
                    randomStartTime.set(Calendar.MINUTE, 0)
                    randomStartTime.add(Calendar.HOUR_OF_DAY, 1)
                }
            }
        }

        if (randomStartTime.after(maxStartTime)) {
            randomStartTime.time = maxStartTime.time
            val minute = randomStartTime.get(Calendar.MINUTE)
            when {
                minute >= 30 -> randomStartTime.set(Calendar.MINUTE, 30)
                else -> randomStartTime.set(Calendar.MINUTE, 0)
            }
        }

        val endCalendar = randomStartTime.clone() as Calendar
        endCalendar.add(Calendar.MINUTE, durationMinutes)

        val timeFormat = SimpleDateFormat("h:mm a", Locale.US)
        val startTime = timeFormat.format(randomStartTime.time)
        val endTime = timeFormat.format(endCalendar.time)

        val hours = durationMinutes / 60
        val minutes = durationMinutes % 60
        val durationText = if (minutes == 0) {
            "$hours hr"
        } else {
            "$hours hr $minutes min"
        }

        val price = if (durationMinutes >= 210) {
            if (Random.nextFloat() < 0.7f) {
                randomFloatBetween(90f, 120f)
            } else {
                randomFloatBetween(120f, 170f)
            }
        } else {
            randomFloatBetween(selectedShift.priceRange.first, selectedShift.priceRange.second)
        }

        val id = "${location}_${startTime}_${durationText}_$price"

        return Offer(
            id = id,
            location = location,
            startTime = startTime,
            endTime = endTime,
            duration = durationText,
            price = String.format("$%.2f", price)
        )
    }

    private fun randomLongBetween(min: Long, max: Long): Long {
        return min + (Random.nextDouble() * (max - min)).toLong()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        refreshJob?.cancel()
        refreshTimeoutHandler.removeCallbacks(refreshTimeoutRunnable)
        _binding = null
    }
}

data class Offer(
    val id: String,
    val location: String,
    val startTime: String,
    val endTime: String,
    val duration: String,
    val price: String
)

class OffersAdapter(
    private val offers: MutableList<Offer>,
    private val onItemClick: (Offer) -> Unit
) : RecyclerView.Adapter<OffersAdapter.OfferViewHolder>() {

    class OfferViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvLocation: TextView = view.findViewById(R.id.tvLocation)
        val tvTimeRange: TextView = view.findViewById(R.id.tvTimeRange)
        val tvDuration: TextView = view.findViewById(R.id.tvDuration)
        val tvPrice: TextView = view.findViewById(R.id.tvPrice)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfferViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.block_item, parent, false)
        return OfferViewHolder(view)
    }

    override fun onBindViewHolder(holder: OfferViewHolder, position: Int) {
        val offer = offers[position]

        holder.tvLocation.text = offer.location
        holder.tvTimeRange.text = "${offer.startTime} - ${offer.endTime}"
        holder.tvDuration.text = offer.duration
        holder.tvPrice.text = offer.price

        holder.itemView.setOnClickListener {
            onItemClick(offer)
        }
    }

    override fun getItemCount() = offers.size
}