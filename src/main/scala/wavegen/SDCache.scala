package wavegen

import chisel3._
import chisel3.util._
import wavegen.misc.Monitor

// TODO: writing

class SDCache(blocks: Int) extends Module {
	override val desiredName = s"SDCache_${blocks}b"

	val io = IO(new Bundle {
		val sd      = SDData()
		val address = Input(UInt(32.W))
		val read    = Input(Bool())
		val dataOut = Valid(UInt(8.W))
	})

	io.sd.doWrite := false.B
	io.sd.dataOut := DontCare

	val invalidBlock = "hffffffff".U(32.W)

	val cache = SyncReadMem(512 * blocks, UInt(8.W))
	val blockIDs = RegInit(VecInit.fill(blocks) { invalidBlock })

	val storedAddress = RegInit(0.U(32.W))

	val sIdle :: sStartReading :: sReading :: Nil = Enum(3)
	val state = RegInit(sIdle)

	def chop(value: UInt): UInt = Cat(value >> 9.U, 0.U(9.W))

	val chosenBlock = RegInit(0.U(log2Ceil(blocks).W))
	val bytePointer = RegInit(0.U(9.W))

	val nextBlock = RegInit(0.U(log2Ceil(blocks).W))

	val cooldown = RegInit(0.U(8.W))

	when (state === sIdle) {

		when (Monitor(io.read)) {
			storedAddress := io.address
			// val found = blockIDs.exists(id => id === chop(io.address))
			// val found = WireInit(invalidBlock)

			state := sStartReading

			blockIDs.zipWithIndex.foreach { case (blockID, i) =>
				when (blockID === chop(io.address)) {
					// chosenBlock := i.U
					// state := sReading
					io.dataOut.valid := true.B
					io.dataOut.bits  := cache(Cat(i.U, io.address(8, 0)))
					state := sIdle
				}
			}
		}

	} .elsewhen (state === sStartReading) {

		bytePointer := 0.U
		chosenBlock := nextBlock
		nextBlock   := nextBlock + 1.U
		state       := sReading

	} .elsewhen (state === sReading) {

		io.sd.doRead := true.B

		when (cooldown =/= 0.U) {
			cooldown := cooldown - 1.U
		} .elsewhen (io.sd.dataIn.valid) {
			cooldown := 8.U // Might be able to set this as low as 4? (100 MHz) / (25 MHz) = 4

			val subindex = storedAddress(8, 0)
			val cacheAddress = Cat(chosenBlock, subindex)

			cache.write(cacheAddress, io.sd.dataIn.bits)

			when (bytePointer === 511.U) {
				io.dataOut.valid := true.B
				io.dataOut.bits  := Mux(subindex === 511.U, io.sd.dataIn.bits, cache(cacheAddress))
				state            := sIdle
			} .otherwise {
				bytePointer := bytePointer + 1.U
			}
		}

	}
}
