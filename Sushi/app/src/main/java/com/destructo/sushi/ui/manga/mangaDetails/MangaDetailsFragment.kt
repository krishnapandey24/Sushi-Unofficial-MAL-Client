package com.destructo.sushi.ui.manga.mangaDetails

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.destructo.sushi.*
import com.destructo.sushi.R
import com.destructo.sushi.adapter.MangaCharacterAdapter
import com.destructo.sushi.adapter.MangaRecommListAdapter
import com.destructo.sushi.adapter.MangaRelatedListAdapter
import com.destructo.sushi.adapter.MangaReviewAdapter
import com.destructo.sushi.databinding.FragmentMangaDetailsBinding
import com.destructo.sushi.enum.mal.UserMangaStatus
import com.destructo.sushi.listener.MalIdListener
import com.destructo.sushi.listener.MangaCharacterListener
import com.destructo.sushi.listener.MangaReviewListener
import com.destructo.sushi.model.mal.common.Genre
import com.destructo.sushi.model.params.MangaUpdateParams
import com.destructo.sushi.network.Status
import com.destructo.sushi.ui.base.BaseFragment
import com.destructo.sushi.ui.manga.MangaUpdateDialog
import com.destructo.sushi.ui.manga.MangaUpdateListener
import com.destructo.sushi.util.*
import com.facebook.ads.*
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_manga_details.view.*
import kotlinx.android.synthetic.main.inc_characters_list.view.*
import kotlinx.android.synthetic.main.inc_genre_list.view.*
import kotlinx.android.synthetic.main.inc_manga_alt_title.view.*
import kotlinx.android.synthetic.main.inc_manga_sub_desc.view.*
import kotlinx.android.synthetic.main.inc_more_manga_detail.view.*
import kotlinx.android.synthetic.main.inc_recomms_list.view.*
import kotlinx.android.synthetic.main.inc_related_manga.view.*
import kotlinx.android.synthetic.main.inc_review_list.view.*
import java.util.*

private const val MANGA_IN_USER_LIST = 1
private const val MANGA_NOT_IN_USER_LIST = 0
private const val USER_MANGA_LIST_DEFAULT = -1

@AndroidEntryPoint
class MangaDetailsFragment : BaseFragment(), MangaUpdateListener, AppBarLayout.OnOffsetChangedListener {

    private val mangaDetailViewModel: MangaDetailViewModel by viewModels()
    private val args: MangaDetailsFragmentArgs by navArgs()

    private lateinit var binding: FragmentMangaDetailsBinding
    private var mangaIdArg: Int = 0

    private lateinit var toolbar: Toolbar
    private lateinit var appBar: AppBarLayout
    private lateinit var collapsingToolbar: CollapsingToolbarLayout
    private lateinit var scoreCardView: MaterialCardView
    private lateinit var coverView: ImageView
    private lateinit var scoreTextView: TextView
    private lateinit var genreChipGroup: ChipGroup
    private lateinit var myListStatus: LinearLayout
    private lateinit var addToListButton: Button
    private lateinit var mangaDetailProgressBar: ProgressBar
    private var isInUserList: Int = USER_MANGA_LIST_DEFAULT
    private lateinit var mangaMoreInfoLayout: ConstraintLayout
    private lateinit var characterSeeMore: TextView
    private lateinit var reviewSeeMore: TextView

    private lateinit var characterAdapter: MangaCharacterAdapter
    private lateinit var relatedAdapter: MangaRelatedListAdapter
    private lateinit var recommAdapter: MangaRecommListAdapter
    private lateinit var reviewAdapter: MangaReviewAdapter

    private var mangaStatus: String? = null
    private var mangaChapters: String? = null
    private var mangaVolumes: String? = null
    private var mangaScore: Int? = 0
    private var mangaStartDate: String? = null
    private var mangaFinishDate: String? = null

    private lateinit var characterRecycler: RecyclerView
    private lateinit var relatedRecycler: RecyclerView
    private lateinit var recommRecycler: RecyclerView
    private lateinit var reviewRecycler: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            mangaIdArg = savedInstanceState.getInt(MANGA_ID_ARG)
            isInUserList = savedInstanceState.getInt("isInUserList")
        } else {
            mangaIdArg = args.mangaId
            mangaDetailViewModel.getMangaDetail(mangaIdArg, false)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(MANGA_ID_ARG, mangaIdArg)
        outState.putInt("isInUserList", isInUserList)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentMangaDetailsBinding.inflate(inflater, container, false)
            .apply { lifecycleOwner = viewLifecycleOwner }

        val snapHelper = PagerSnapHelper()
        characterRecycler = binding.root.characterRecycler
        characterRecycler.addItemDecoration(ListItemHorizontalDecor(LIST_SPACE_HEIGHT))
        reviewRecycler = binding.root.reviewsRecycler
        snapHelper.attachToRecyclerView(reviewRecycler)
        relatedRecycler = binding.root.relatedMangaRecycler
        relatedRecycler.addItemDecoration(ListItemHorizontalDecor(LIST_SPACE_HEIGHT))
        recommRecycler = binding.root.recommRecycler
        recommRecycler.addItemDecoration(ListItemHorizontalDecor(LIST_SPACE_HEIGHT))
        myListStatus = binding.root.my_manga_status
        addToListButton = binding.root.add_manga_to_list
        mangaDetailProgressBar = binding.root.manga_detail_progress
        mangaMoreInfoLayout = binding.root.manga_more_detail_layout
        toolbar = binding.mangaDescToolbar
        appBar = binding.mangaAppBar
        collapsingToolbar = binding.mangaCollapsingToolbar
        scoreCardView = binding.mangaScoreFab
        scoreTextView = binding.mangaScoreTxt
        coverView = binding.root.manga_desc_cover_img
        genreChipGroup = binding.root.genre_chip_group
        characterSeeMore = binding.root.charactersMore
        reviewSeeMore = binding.root.reviewsMore
        setupListeners()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupToolbar()
        initialiseAdapters()

        mangaDetailViewModel.mangaDetail.observe(viewLifecycleOwner) { resource ->

            when (resource.status) {
                Status.LOADING -> {
                    mangaDetailProgressBar.visibility = View.VISIBLE
                }
                Status.SUCCESS -> {
                    mangaDetailProgressBar.visibility = View.GONE
                    resource.data?.let { manga ->
                        mangaDetailViewModel.getMangaCharacters(mangaIdArg)
                        mangaDetailViewModel.getMangaReviews(mangaIdArg, "1")

                        binding.mangaEntity = manga

                        if (manga.myMangaListStatus != null) {
                            myListStatus.visibility = View.VISIBLE
                            isInUserList = MANGA_IN_USER_LIST
                            mangaScore = manga.myMangaListStatus.score
                            mangaStatus = manga.myMangaListStatus.status?.toTitleCase()
                            mangaChapters = manga.myMangaListStatus.numChaptersRead.toString()
                            mangaVolumes = manga.myMangaListStatus.numVolumesRead.toString()
                            mangaStartDate = manga.myMangaListStatus.startDate
                            mangaFinishDate = manga.myMangaListStatus.finishDate

                        } else {
                            isInUserList = MANGA_NOT_IN_USER_LIST
                        }

                        manga.mainPicture?.medium?.let { setScoreCardColor(it) }
                        manga.genres?.let { setGenreChips(it) }
                        recommAdapter.submitList(manga.recommendations)
                        relatedAdapter.submitList(manga.relatedManga)
                    }
                }
                Status.ERROR -> {
                }
            }
        }

        mangaDetailViewModel.mangaCharacter.observe(viewLifecycleOwner) { resource ->

            when (resource.status) {
                Status.LOADING -> {
                }
                Status.SUCCESS -> {
                    resource.data?.let { characters ->
                        characterAdapter.submitList(characters.characters)
                    }
                }
                Status.ERROR -> {
                }
            }
        }

        mangaDetailViewModel.mangaReview.observe(viewLifecycleOwner) { resource ->

            when (resource.status) {
                Status.LOADING -> {
                }
                Status.SUCCESS -> {
                    resource.data?.let { mangaReview ->
                        reviewAdapter.submitList(mangaReview.reviews)
                    }
                }
                Status.ERROR -> {
                }
            }
        }

        mangaDetailViewModel.userMangaStatus.observe(viewLifecycleOwner) { resource ->
            when (resource.status) {
                Status.LOADING -> {
                }
                Status.SUCCESS -> {

                    changeButtonState(addToListButton, true)
                    isInUserList = MANGA_IN_USER_LIST
                    resource.data?.let { mangaStatus ->
                        addToListButton.text = mangaStatus.status.toString().toTitleCase()
                        addToListButton.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.ic_check_fill,
                            0,
                            0,
                            0
                        )
                        myListStatus.visibility = View.VISIBLE
                        mangaDetailViewModel.getMangaDetail(mangaIdArg, true)
                    }
                }
                Status.ERROR -> {
                    changeButtonState(addToListButton, true)
                }
            }
        }

        mangaDetailViewModel.userMangaRemove.observe(viewLifecycleOwner) {
            when (it.status) {
                Status.ERROR -> {
                }
                Status.SUCCESS -> {
                    addToListButton.text = getString(R.string.add_to_list)
                    addToListButton.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_add_fill,
                        0,
                        0,
                        0
                    )
                    myListStatus.visibility = View.GONE
                    mangaDetailViewModel.getMangaDetail(mangaIdArg, true)
                }
                Status.LOADING -> {
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        appBar.addOnOffsetChangedListener(this)
    }

    private fun setupListeners() {
        setNavigationListener()
        setMoreInfoLayoutListener()
        setAddToListListener()
        setCollapsingToolbarListener()
    }

    private fun setCollapsingToolbarListener() {
        collapsingToolbar.setOnLongClickListener {
            copyMangaTitleToClipBoard()
            return@setOnLongClickListener false
        }
    }

    private fun setAddToListListener() {
        addToListButton.setOnClickListener {

            when (isInUserList) {
                USER_MANGA_LIST_DEFAULT -> {
                }
                MANGA_IN_USER_LIST -> {
                    val myDialog = MangaUpdateDialog.newInstance(
                        mangaStatus,
                        mangaChapters,
                        mangaVolumes,
                        mangaScore ?: 0,
                        mangaStartDate, mangaFinishDate
                    )
                    myDialog.show(childFragmentManager, "mangaUpdateDialog")
                }
                MANGA_NOT_IN_USER_LIST -> {
                    mangaDetailViewModel.updateUserMangaStatus(
                        MangaUpdateParams(
                            mangaId = mangaIdArg.toString(),
                            status = UserMangaStatus.PLAN_TO_READ.value
                        )
                    )
                }
            }
        }
    }

    private fun setMoreInfoLayoutListener() {
        mangaMoreInfoLayout.setOnClickListener {
            if (it.manga_more_detail_view.visibility != View.VISIBLE) {
                it.manga_more_detail_view.visibility = View.VISIBLE
            } else {
                it.manga_more_detail_view.visibility = View.GONE
            }
        }
    }

    private fun setNavigationListener() {
        characterSeeMore.setOnClickListener {
            findNavController().navigate(
                R.id.mangaCharactersFragment,
                bundleOf(Pair(MANGA_ID_ARG, mangaIdArg)),
                getAnimNavOptions()
            )
        }
        reviewSeeMore.setOnClickListener {
            findNavController().navigate(
                R.id.mangaReviewsFragment,
                bundleOf(Pair(MANGA_ID_ARG, mangaIdArg)),
                getAnimNavOptions()
            )
        }
    }

    private fun initialiseAdapters() {
        recommAdapter = MangaRecommListAdapter(MalIdListener {
            it?.let { navigateToMangaDetails(it) }
        })
        relatedAdapter = MangaRelatedListAdapter(MalIdListener {
            it?.let { navigateToMangaDetails(it) }
        })
        characterAdapter = MangaCharacterAdapter(MangaCharacterListener {
            it?.let { it.malId?.let { it1 -> navigateToCharacterDetails(it1) } }
        })
        reviewAdapter = MangaReviewAdapter(MangaReviewListener {
            it?.let {
                val reviewDialog = MangaReviewBottomSheetFragment.newInstance(it)
                reviewDialog.show(childFragmentManager, "manga_review_dialog")
            }
        })
        characterRecycler.adapter = characterAdapter
        reviewRecycler.adapter = reviewAdapter
        recommRecycler.adapter = recommAdapter
        relatedRecycler.adapter = relatedAdapter
    }

    private fun setGenreChips(genreList: List<Genre?>) {
        genreChipGroup.removeAllViews()
        genreList.forEach { genre ->
            genre?.let {
                val chip = Chip(context, null, R.attr.customChipStyle)
                chip.text = it.name
                genreChipGroup.addView(chip)
            }
        }
    }

    private fun setScoreCardColor(imgUrl: String) {
        Glide.with(this)
            .asBitmap()
            .load(imgUrl)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: com.bumptech.glide.request.transition.Transition<in Bitmap>?
                ) {
                    Palette.from(resource).generate { palette: Palette? ->

                        if (palette?.vibrantSwatch != null) {
                            palette.vibrantSwatch?.rgb?.let { color ->
                                scoreCardView.setCardBackgroundColor(color)
                            }
                            palette.vibrantSwatch?.titleTextColor?.let {
                                scoreTextView.setTextColor(it)
                            }
                        } else {
                            context?.let {
                                scoreCardView.setCardBackgroundColor(it.getColorFromAttr(R.attr.scoreCardBackground))
                                scoreTextView.setTextColor(it.getColorFromAttr(R.attr.scoreCardText))
                            }
                        }

                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {

                }
            })
    }

    private fun navigateToMangaDetails(malId: Int) {
        this.findNavController().navigate(
            R.id.mangaDetailsFragment,
            bundleOf(Pair(MANGA_ID_ARG, malId)),
            getAnimNavOptions()
        )
    }

    private fun navigateToCharacterDetails(character: Int) {
        this.findNavController().navigate(
            R.id.characterFragment,
            bundleOf(Pair(CHARACTER_ID_ARG, character)),
            getAnimNavOptions()
        )
    }

    private fun convertStatus(data: String): String {
        var status = ""

        when (data) {
            getString(R.string.reading) -> {
                status = UserMangaStatus.READING.value
            }
            getString(R.string.completed) -> {
                status = UserMangaStatus.COMPLETED.value
            }
            getString(R.string.plan_to_read) -> {
                status = UserMangaStatus.PLAN_TO_READ.value
            }
            getString(R.string.dropped) -> {
                status = UserMangaStatus.DROPPED.value
            }
            getString(R.string.on_hold) -> {
                status = UserMangaStatus.ON_HOLD.value
            }
        }
        return status
    }

    private fun changeButtonState(button: Button, status: Boolean) {
        if (status) {
            button.isEnabled = true
            button.setTextColor(context?.let {
                AppCompatResources.getColorStateList(
                    it,
                    R.color.textColorOnPrimary
                )
            })
        } else {
            button.isEnabled = false
            button.setTextColor(context?.let {
                AppCompatResources.getColorStateList(
                    it,
                    R.color.textColorOnPrimary
                )
            })
        }
    }

    override fun onUpdateClick(
        status: String,
        chapters: Int,
        volume: Int,
        score: Int,
        remove: Boolean,
        startDate: String?,
        finishDate: String?
    ) {
        if (!remove) {
            mangaDetailViewModel.updateUserMangaStatus(
                MangaUpdateParams(
                    mangaId = mangaIdArg.toString(),
                    status = convertStatus(status),
                    num_chapters_read = chapters,
                    num_volumes_read = volume,
                    score = score,
                    start_date = startDate,
                    finish_date = finishDate,
                    totalChapters = mangaDetailViewModel.mangaDetail.value?.data?.numChapters,
                    totalVolumes = mangaDetailViewModel.mangaDetail.value?.data?.numVolumes
                )
            )
        } else {
            mangaDetailViewModel.removeAnime(mangaIdArg)
        }
    }

    override fun onOffsetChanged(appBarLayout: AppBarLayout?, verticalOffset: Int) {
        if (verticalOffset == 0) {
            var drawable: Drawable? = toolbar.navigationIcon
            drawable?.let {
                drawable = DrawableCompat.wrap(drawable!!)
                context?.let { it1 -> ContextCompat.getColor(it1, R.color.iconTintOnPrimary) }
                    ?.let { it2 ->
                        DrawableCompat.setTint(
                            drawable!!.mutate(),
                            it2
                        )
                    }
                toolbar.navigationIcon = drawable
                toolbar.overflowIcon =
                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_more_fill_light)
            }

        } else {
            toolbar.setNavigationIcon(R.drawable.ic_arrow_back_line)
            toolbar.overflowIcon =
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_more_fill)
        }
    }


    private fun setupToolbar() {
        toolbar.setOnLongClickListener {
            copyMangaTitleToClipBoard()
            return@setOnLongClickListener false
        }
        toolbar.setNavigationOnClickListener { view ->
            view.findNavController().navigateUp()
        }

        toolbar.inflateMenu(R.menu.detail_menu_options)
        toolbar.setOnMenuItemClickListener { item ->
            when (item?.itemId) {
                R.id.share_item ->  shareManga()
                R.id.copy_title ->  copyMangaTitleToClipBoard()
                R.id.open_in_browser ->  openInBrowser()
            }

            false
        }
    }

    private fun shareManga() {
        mangaDetailViewModel.mangaDetail.value?.data?.title?.let {
            val url = BASE_MAL_MANGA_URL + mangaIdArg
            val data = String.format(getString(R.string.share_anime_or_manga), it, url)
            context?.shareText(data)
        }
    }

    private fun openInBrowser() {
        val url = BASE_MAL_MANGA_URL + mangaIdArg
        context?.openUrl(url)
    }

    private fun copyMangaTitleToClipBoard() {
        mangaDetailViewModel.mangaDetail.value?.data?.title?.let{
            context?.copyToClipboard(it)
            context?.makeShortToast("${getString(R.string.copied_to_clipboard)}\n$it")
        }
    }


}