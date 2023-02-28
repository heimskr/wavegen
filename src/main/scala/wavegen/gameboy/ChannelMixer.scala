package wavegen.gameboy

import chisel3._
import chisel3.util._

// Original single-cycle algorithm:
//     Seq(io.outR, io.outL).zipWithIndex.foreach { case (out, i) =>
//         val to_mult = (channels.zipWithIndex.map { case (channel, j) =>
//             Mux(registers.NR51(3 + 4 * i - j), channel, 0.U(4.W))
//         }.foldLeft(0.U)(_ +& _))(7, 0)
//         out := (registers.NR50(2 + 4 * i, 4 * i) * to_mult +& to_mult) >> 3.U
//     }

// In pseudocode:
//     For each side (left, right):
//         Let sum = 0
//         For each channel (0, 1, 2, 3):
//             If NR51((left? 3 : 7) - channel.id), then sum += channel.out
//         If sum != 0,
//             Let adjusted = NR50(left? 6 : 2, left? 4 : 0) * sum
//             adjusted += sum
//             adjusted >>= 3
//             Output(side) = adjusted
//         Else,
//             Output(side) = 0

class ChannelMixer(inWidth: Int) extends Module {
	val outWidth = 2 + inWidth + 3 // 2 extra bits from summing 2^2 values, 3 extra bits from multiplying by 2^3

	val io = IO(new Bundle {
		val in   = Flipped(Valid(Vec(4, UInt(inWidth.W)))) // (0) => channel0, (1) => channel1, ...
		val nr50 = Input(UInt(8.W))
		val nr51 = Input(UInt(8.W))
		val out  = Valid(new Bundle {
			val left  = UInt(outWidth.W)
			val right = UInt(outWidth.W)
		})
	})

	val sIdle :: sS0 :: sS1 :: sS2 :: sS3 :: sA :: Nil = Enum(6)

	val state = RegInit(sIdle)
	val valid = RegInit(false.B)
	val left  = RegInit(0.U(outWidth.W))
	val right = RegInit(0.U(outWidth.W))

	io.out.valid      := false.B
	io.out.bits.left  := left
	io.out.bits.right := right

	when (state === sIdle) {

		io.out.valid := valid

		when (io.in.valid) {
			left         := 0.U
			right        := 0.U
			state        := sS0
			valid        := false.B
			io.out.valid := false.B
		}

	} .elsewhen (state === sS0) {

		when (io.nr51(3.U)) { left  := io.in.bits(0) }
		when (io.nr51(7.U)) { right := io.in.bits(0) }
		state := sS1

	} .elsewhen (state === sS1) {

		when (io.nr51(2.U)) { left  := left  + io.in.bits(1) }
		when (io.nr51(6.U)) { right := right + io.in.bits(1) }
		state := sS2

	} .elsewhen (state === sS2) {

		when (io.nr51(1.U)) { left  := left  + io.in.bits(2) }
		when (io.nr51(5.U)) { right := right + io.in.bits(2) }
		state := sS3

	} .elsewhen (state === sS3) {

		when (io.nr51(0.U)) { left  := left  + io.in.bits(3) }
		when (io.nr51(4.U)) { right := right + io.in.bits(3) }
		state := sA

	} .elsewhen (state === sA) {

		left  := (io.nr50(6, 4) * left)  + left
		right := (io.nr50(2, 0) * right) + right
		state := sIdle
		valid := true.B
		io.out.valid := true.B

	}
}
