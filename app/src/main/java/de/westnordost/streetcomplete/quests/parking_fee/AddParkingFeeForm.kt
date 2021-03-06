package de.westnordost.streetcomplete.quests.parking_fee

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.view.isGone
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.westnordost.streetcomplete.Injector
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.ktx.toObject
import de.westnordost.streetcomplete.quests.AbstractQuestFormAnswerFragment
import de.westnordost.streetcomplete.quests.OtherAnswer
import de.westnordost.streetcomplete.quests.opening_hours.adapter.AddOpeningHoursAdapter
import de.westnordost.streetcomplete.quests.opening_hours.adapter.OpeningHoursRow
import de.westnordost.streetcomplete.util.AdapterDataChangedWatcher
import de.westnordost.streetcomplete.util.Serializer
import kotlinx.android.synthetic.main.fragment_quest_answer.*
import kotlinx.android.synthetic.main.quest_buttonpanel_yes_no.*
import kotlinx.android.synthetic.main.quest_fee_hours.*
import java.util.*
import javax.inject.Inject

class AddParkingFeeForm : AbstractQuestFormAnswerFragment<FeeAnswer>() {

    override val contentLayoutResId = R.layout.quest_fee_hours
    override val buttonsResId = R.layout.quest_buttonpanel_yes_no

    override val otherAnswers = listOf(
        OtherAnswer(R.string.quest_fee_answer_hours) { isDefiningHours = true }
    )

    private lateinit var openingHoursAdapter: AddOpeningHoursAdapter

    private lateinit var content: ViewGroup

    private var isDefiningHours: Boolean = false
    set(value) {
        field = value

        content.isGone = !value
        noButton?.isGone = value
        yesButton?.isGone = value
    }
    private var isFeeOnlyAtHours: Boolean = false

    @Inject internal lateinit var serializer: Serializer

    init {
        Injector.applicationComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        openingHoursAdapter = AddOpeningHoursAdapter(requireContext(), countryInfo)
        openingHoursAdapter.rows = loadOpeningHoursData(savedInstanceState).toMutableList()
        openingHoursAdapter.registerAdapterDataObserver( AdapterDataChangedWatcher { checkIsFormComplete() })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        content = view.findViewById(R.id.content)

        // must be read here because setting these values effects the UI
        isFeeOnlyAtHours = savedInstanceState?.getBoolean(IS_FEE_ONLY_AT_HOURS, true) ?: true
        isDefiningHours = savedInstanceState?.getBoolean(IS_DEFINING_HOURS) ?: false

        okButton.setOnClickListener { onClickOk() }
        yesButton.setOnClickListener { onClickYesNo(true) }
        noButton.setOnClickListener { onClickYesNo(false) }

        openingHoursList.layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        openingHoursList.adapter = openingHoursAdapter
        openingHoursList.isNestedScrollingEnabled = false
        checkIsFormComplete()

        addTimesButton.setOnClickListener { openingHoursAdapter.addNewWeekdays() }

        val spinnerItems = listOf(
            getString(R.string.quest_fee_only_at_hours),
            getString(R.string.quest_fee_not_at_hours)
        )
        selectFeeOnlyAtHours.adapter = ArrayAdapter(requireContext(), R.layout.spinner_item_centered, spinnerItems)
        selectFeeOnlyAtHours.setSelection(if (isFeeOnlyAtHours) 0 else 1)
        selectFeeOnlyAtHours.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                isFeeOnlyAtHours = position == 0
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    override fun onClickOk() {
        val times = openingHoursAdapter.createOpeningHours()
        if (times.rules.isNotEmpty()) {
            if(isFeeOnlyAtHours) {
                applyAnswer(HasFeeAtHours(times))
            } else {
                applyAnswer(HasFeeExceptAtHours(times))
            }
        } else {
            onClickYesNo(!isFeeOnlyAtHours)
        }
    }

    private fun onClickYesNo(answer: Boolean) {
        applyAnswer(if(answer) HasFee else HasNoFee)
    }

    private fun loadOpeningHoursData(savedInstanceState: Bundle?): List<OpeningHoursRow> =
        if (savedInstanceState != null) {
            serializer.toObject<ArrayList<OpeningHoursRow>>(savedInstanceState.getByteArray(OPENING_HOURS_DATA)!!)
        } else {
            listOf()
        }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putByteArray(OPENING_HOURS_DATA, serializer.toBytes(ArrayList(openingHoursAdapter.rows)))
        outState.putBoolean(IS_DEFINING_HOURS, isDefiningHours)
        outState.putBoolean(IS_FEE_ONLY_AT_HOURS, isFeeOnlyAtHours)
    }

    override fun isFormComplete() =
        isDefiningHours && openingHoursAdapter.rows.isNotEmpty()

    companion object {
        private const val OPENING_HOURS_DATA = "oh_data"
        private const val IS_FEE_ONLY_AT_HOURS = "oh_fee_only_at"
        private const val IS_DEFINING_HOURS = "oh"
    }
}
