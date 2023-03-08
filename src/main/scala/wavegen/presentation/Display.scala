package wavegen.presentation

import chisel3._

class Display extends BlackBox {
	val io = IO(new Bundle {
		val clk           = Input(Clock())
		val clk_pix1      = Input(Clock())
		val clk_pix5      = Input(Clock())
		val clk30         = Input(Clock())
		val rst_n         = Input(Reset())
		val audioL        = Input(UInt(24.W))
		val audioR        = Input(UInt(24.W))
		val hdmi_tx_clk_n = Output(Bool())
		val hdmi_tx_clk_p = Output(Bool())
		val hdmi_tx_n     = Output(UInt(3.W))
		val hdmi_tx_p     = Output(UInt(3.W))
		val x             = Output(UInt(11.W))
		val y             = Output(UInt(10.W))
		val red           = Input(UInt(8.W))
		val green         = Input(UInt(8.W))
		val blue          = Input(UInt(8.W))
	})
}
