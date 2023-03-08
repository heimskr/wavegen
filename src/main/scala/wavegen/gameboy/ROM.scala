package wavegen.gameboy

import chisel3._

class blk_mem_gen_0(addressWidth: Int, romWidth: Int) extends BlackBox {
	val io = IO(new Bundle {
		val clka = Input(Clock())
		val addra = Input(UInt(addressWidth.W))
		val douta = Output(UInt(romWidth.W))
	})
}
