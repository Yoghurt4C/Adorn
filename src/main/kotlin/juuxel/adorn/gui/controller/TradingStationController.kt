package juuxel.adorn.gui.controller

import io.github.cottonmc.cotton.gui.client.BackgroundPainter
import io.github.cottonmc.cotton.gui.widget.WGridPanel
import io.github.cottonmc.cotton.gui.widget.WItemSlot
import juuxel.adorn.block.entity.TradingStation
import juuxel.adorn.client.gui.painter.Painters
import juuxel.adorn.gui.AdornGuis
import juuxel.adorn.gui.widget.WCenteredLabel
import juuxel.adorn.trading.Trade
import juuxel.adorn.trading.TradeInventory
import juuxel.adorn.util.Colors
import juuxel.adorn.util.color
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.text.TranslatableText
import org.apache.logging.log4j.LogManager

class TradingStationController(
    syncId: Int,
    playerInv: PlayerInventory,
    private val context: ScreenHandlerContext,
    private val forOwner: Boolean
) : BaseAdornController(
    AdornGuis.TRADING_STATION,
    syncId,
    playerInv,
    context,
    getStorage(context),
    getBlockPropertyDelegate(context)
) {
    private val slotWidgets: List<WItemSlot>

    init {
        (rootPanel as WGridPanel).apply {
            titleColor = Colors.WHITE

            val tradeInv = getTrade(context).createInventory()

            val mutableSlots = ArrayList<WItemSlot>()
            fun WItemSlot.addToSlots() = apply { mutableSlots += this }

            add(WItemSlot.of(tradeInv, 0).setModifiable(false).addToSlots(), 1, 2)
            add(WItemSlot.of(tradeInv, 1).setModifiable(false).addToSlots(), 1, 4)

            add(WCenteredLabel(TranslatableText("block.adorn.trading_station.selling"), Colors.WHITE), 1, 1)
            add(WCenteredLabel(TranslatableText("block.adorn.trading_station.price"), Colors.WHITE), 1, 3)

            add(WItemSlot.of(blockInventory, 0, 4, 3).addToSlots(), 3, 2)

            add(playerInvPanel, 0, 6)
            validate(this@TradingStationController)

            slotWidgets = mutableSlots
        }
    }

    override fun onSlotClick(slotNumber: Int, button: Int, action: SlotActionType, player: PlayerEntity): ItemStack {
        val slot = slots.getOrNull(slotNumber)
        val cursorStack = player.inventory.cursorStack

        return if (forOwner && slot?.inventory is TradeInventory) {
            when (action) {
                SlotActionType.PICKUP -> {
                    slot.stack = cursorStack.copy()
                    slot.markDirty()

                    if (!world.isClient) {
                        (getTradingStation(context) as? BlockEntityClientSerializable)?.sync() ?: run {
                            val exception = Exception("Stack trace")
                            exception.fillInStackTrace()
                            LOGGER.warn("[Adorn] Could not sync empty trading station, report this!", exception)
                        }
                    }

                    cursorStack
                }

                else -> cursorStack
            }
        } else if (forOwner || (slot?.inventory is PlayerInventory && action != SlotActionType.QUICK_MOVE)) {
            super.onSlotClick(slotNumber, button, action, player)
        } else cursorStack
    }

    @Environment(EnvType.CLIENT)
    override fun addPainters() {
        super.addPainters()
        rootPanel.backgroundPainter = BackgroundPainter.createColorful(color(0x359668))
        slotWidgets.forEach { it.setBackgroundPainter(Painters.LIBGUI_STYLE_SLOT) }
    }

    override fun getTitleColor() = Colors.WHITE

    companion object {
        private val LOGGER = LogManager.getLogger()

        /**
         * Gets the [juuxel.adorn.block.entity.TradingStationBlockEntity] at the [context]'s location.
         * If it's not present, creates an empty trading station using [TradingStation.createEmpty].
         */
        private fun getTradingStation(context: ScreenHandlerContext) =
            getBlockEntity(context) as? TradingStation ?: run {
                LOGGER.warn("[Adorn] Trading station not found, creating fake one")
                TradingStation.createEmpty()
            }

        /**
         * Gets the [TradingStation.storage] of the trading station at the [context]'s location.
         * Uses [getTradingStation] for finding a trading station.
         */
        private fun getStorage(context: ScreenHandlerContext): Inventory = getTradingStation(context).storage

        /**
         * Gets the [TradingStation.trade] of the trading station at the [context]'s location.
         * Uses [getTradingStation] for finding a trading station.
         */
        private fun getTrade(context: ScreenHandlerContext): Trade = getTradingStation(context).trade
    }
}
