package wavegen

import chisel3._
import chisel3.util._
import wavegen.misc.BoolMonitor
import wavegen.misc.Monitor

// TODO: writing

class SDCache(blocks: Int) extends Module {
	override val desiredName = s"SDCache_${blocks}b"

	val io = IO(new Bundle {
		val sdClock = Input(Clock())
		val sd      = SDData()
		val address = Input(UInt(32.W))
		val read    = Input(Bool())
		val dataOut = Valid(UInt(8.W))
		val state   = Output(UInt(2.W))
	})

	val doRead  = RegInit(false.B)
	val address = RegInit(0.U(32.W))
	val dataOut = RegInit(0.U.asTypeOf(Valid(UInt(8.W))))
	val readAck = RegInit(false.B)

	io.sd.doWrite    := false.B
	io.sd.doRead     := doRead
	io.sd.dataOut    := DontCare
	io.sd.address    := address
	io.sd.readAck    := readAck
	io.dataOut.valid := dataOut.valid
	io.dataOut.bits  := dataOut.bits

	val invalidBlock = "hffffffff".U(32.W)

	val cache = SyncReadMem(512 * blocks, UInt(8.W))
	val blockIDs = RegInit(VecInit.fill(blocks) { invalidBlock })

	val storedAddress = RegInit(0.U(32.W))

	val sIdle :: sStartReading :: sReading :: Nil = Enum(3)
	val state = RegInit(sIdle)
	io.state := state

	def chop(value: UInt): UInt = Cat(value >> 9.U, 0.U(9.W))

	val chosenBlock = RegInit(0.U(log2Ceil(blocks).W))
	val bytePointer = RegInit(0.U(9.W))

	val nextBlock = RegInit(0.U(log2Ceil(blocks).W))

	val cooldown = RegInit(0.U(8.W))

	when (state === sIdle) {

		doRead  := false.B
		readAck := false.B

		when (io.read) {
			storedAddress := io.address
			// val found = blockIDs.exists(id => id === chop(io.address))
			// val found = WireInit(invalidBlock)

			state := sStartReading

			blockIDs.zipWithIndex.foreach { case (blockID, i) =>
				when (blockID === chop(io.address)) {
					// chosenBlock := i.U
					// state := sReading
					dataOut.valid := true.B
					dataOut.bits  := cache(Cat(i.U, io.address(8, 0)))
					state         := sIdle
				}
			}
		}

	} .elsewhen (state === sStartReading) {

		doRead      := false.B
		readAck     := false.B
		bytePointer := 0.U
		chosenBlock := nextBlock
		nextBlock   := nextBlock + 1.U
		state       := sReading

	} .elsewhen (state === sReading) {

		doRead  := false.B
		address := chop(storedAddress) + bytePointer

		when (cooldown === 1.U) {
			readAck  := false.B
			cooldown := 0.U
		} .elsewhen (cooldown =/= 0.U) {
			readAck  := true.B
			cooldown := cooldown - 1.U
		} .elsewhen (io.sd.dataIn.valid && io.sd.ready) {
			doRead   := false.B
			readAck  := true.B
			cooldown := 8.U // Might be able to set this as low as 2? (100 MHz) / (50 MHz) = 2

			val subindex = storedAddress(8, 0)
			val cacheAddress = Cat(chosenBlock, subindex)

			cache.write(cacheAddress, io.sd.dataIn.bits)

			when (bytePointer === 511.U) {
				dataOut.valid := true.B
				dataOut.bits  := Mux(subindex === 511.U, io.sd.dataIn.bits, cache(cacheAddress))
				state         := sIdle
			} .otherwise {
				bytePointer := bytePointer + 1.U
			}
		} .otherwise {
			doRead  := true.B
			readAck := false.B
		}

	}
}
