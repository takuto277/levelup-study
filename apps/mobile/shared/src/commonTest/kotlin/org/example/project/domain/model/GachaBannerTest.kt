package org.example.project.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GachaBannerTest {

    @Test
    fun primaryFeaturedForHeroPrefersFeaturedWithImageUrl() {
        val characterWithoutImage = featured("character", GachaResultType.CHARACTER)
        val weaponWithImage = featured("weapon", GachaResultType.WEAPON, imageUrl = "https://example.com/weapon.png")

        val selected = banner(listOf(characterWithoutImage, weaponWithImage)).primaryFeaturedForHero()

        assertEquals(weaponWithImage, selected)
    }

    @Test
    fun primaryFeaturedForHeroFallsBackToCharacterWhenNoImageUrlExists() {
        val weapon = featured("weapon", GachaResultType.WEAPON)
        val character = featured("character", GachaResultType.CHARACTER)

        val selected = banner(listOf(weapon, character)).primaryFeaturedForHero()

        assertEquals(character, selected)
    }

    @Test
    fun primaryFeaturedForHeroFallsBackToFirstFeaturedWhenNoCharacterExists() {
        val weapon = featured("weapon", GachaResultType.WEAPON)
        val costume = featured("costume", GachaResultType.COSTUME)

        val selected = banner(listOf(weapon, costume)).primaryFeaturedForHero()

        assertEquals(weapon, selected)
    }

    @Test
    fun primaryFeaturedForHeroReturnsNullWhenFeaturedIsEmpty() {
        assertNull(banner(emptyList()).primaryFeaturedForHero())
    }

    private fun banner(featured: List<GachaBannerFeatured>): GachaBanner = GachaBanner(
        id = "banner",
        name = "テストバナー",
        bannerType = BannerType.MIXED,
        startAt = "2026-06-01",
        endAt = "2026-06-30",
        pityThreshold = null,
        rateTable = "{}",
        isActive = true,
        featured = featured,
    )

    private fun featured(
        id: String,
        itemType: GachaResultType,
        imageUrl: String = "",
    ): GachaBannerFeatured = GachaBannerFeatured(
        id = id,
        bannerId = "banner",
        itemId = "item-$id",
        itemType = itemType,
        rateUp = 1f,
        itemName = id,
        rarity = 5,
        imageUrl = imageUrl,
    )
}
