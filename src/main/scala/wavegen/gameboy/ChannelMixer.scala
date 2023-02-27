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

class ChannelMixer extends Module {
	val io = IO(new Bundle {
		val in   = Flipped(Valid(Vec(4, UInt(4.W)))) // (0) => channel0, (1) => channel1, ...
		val nr50 = Input(UInt(8.W))
		val nr51 = Input(UInt(8.W))
		val out  = Valid(new Bundle {
			val left  = UInt(4.W)
			val right = UInt(4.W)
		})
	})

	val sIdle :: sSumLeft :: sAdjustLeft :: sSumRight :: sAdjustRight :: Nil = Enum(5)

	val state = RegInit(sIdle)

	val valid    = RegInit(false.B)
	val channel  = RegInit(0.U(2.W))
	val left     = RegInit(0.U(7.W))
	val right    = RegInit(0.U(7.W))

	io.out.valid      := false.B
	io.out.bits.left  := left
	io.out.bits.right := right

	when (state === sIdle) {

		io.out.valid := valid

		when (io.in.valid) {
			left         := Mux(io.nr51(3), io.in.bits(0), 0.U(4.W))
			right        := 0.U
			channel      := 1.U
			state        := sSumLeft
			valid        := false.B
			io.out.valid := false.B
		}

	} .elsewhen (state === sSumLeft) {

		when (io.nr51(3.U - channel)) {
			left := left + io.in.bits(channel)
		}

		when (channel === 3.U) {
			channel := 0.U
			state   := sAdjustLeft
		} .otherwise {
			channel := channel + 1.U
		}

	} .elsewhen (state === sAdjustLeft) {

		left := ((io.nr50(6, 4) * left) + left) >> 3.U
		state := sSumRight

	} .elsewhen (state === sSumRight) {

		when (io.nr51(3.U - channel)) {
			right := right + io.in.bits(channel)
		}

		when (channel === 3.U) {
			channel := 0.U
			state   := sAdjustRight
		} .otherwise {
			channel := channel + 1.U
		}

	} .elsewhen (state === sAdjustRight) {

		right := ((io.nr50(2, 0) * right) + right) >> 3.U
		state := sIdle
		valid := true.B

	}
}
