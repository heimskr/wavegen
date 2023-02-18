package wavegen

import chisel3._
import chisel3.util._

class Divider(width: Int) extends Module {

	val io = IO(new Bundle {
		val in  = Flipped(Decoupled(new Bundle {
			val numerator   = UInt(width.W)
			val denominator = UInt(width.W)
		}))
		val out = Valid(new Bundle {
			val quotient = UInt(width.W)
			val remainder = UInt(width.W)
		})
	})

	if (width < 4) {
		io.out.valid := true.B
		io.in.ready  := true.B
		io.out.bits.quotient  := io.in.bits.numerator / io.in.bits.denominator
		io.out.bits.remainder := io.in.bits.numerator % io.in.bits.denominator
	} else {
		def leadingZeros(x: UInt): UInt = {
			val out = Wire(chiselTypeOf(x))
			val priority = PriorityEncoder(Reverse(x))

			when (x === 0.U) {
				out := x.getWidth.U
			} .otherwise {
				out := priority
			}

			out
		}

		val working     = RegInit(false.B)
		val valid       = RegInit(false.B)
		val numerator   = Reg(UInt((2 * width + 1).W))
		val denominator = Reg(UInt(width.W))
		val initNumer   = Reg(UInt(width.W))
		val initDenom   = Reg(UInt(width.W))
		val k           = Reg(UInt(log2Ceil(width).W))
		val i           = Reg(UInt(log2Ceil(width + 1).W))
		val quotient    = Reg(UInt(width.W))
		val remainder   = Reg(UInt(width.W))
		val ap          = Reg(UInt(width.W))
		val an          = Reg(UInt(width.W))

		when (io.in.valid && !working) {
			val zeros = leadingZeros(io.in.bits.denominator)
			working     := true.B
			valid       := false.B
			numerator   := io.in.bits.numerator << zeros
			denominator := io.in.bits.denominator << zeros
			initNumer   := io.in.bits.numerator
			initDenom   := io.in.bits.denominator
			k           := zeros
			i           := 0.U
			quotient    := 0.U
			remainder   := 0.U
			ap          := 0.U
			an          := 0.U
		}

		io.out.valid := valid

		when (working) {
			io.in.ready := false.B

			when (i === width.U) {
				when (numerator.head(1) === 1.U) {
					quotient := ap - an - 1.U
					val rem = ((numerator >> width.U) + denominator) >> k
					// Ugly hack?
					when (initDenom <= rem) {
						remainder := rem - (1.U << ((width + 1).U - PriorityEncoder(Reverse(initDenom))))
					} .otherwise {
						remainder := rem
					}
				} .otherwise {
					quotient := ap - an
					remainder := (numerator >> width.U) >> k
				}
				
				working := false.B
				valid   := true.B
			} .otherwise {
				val top3 = numerator(2 * width, 2 * width - 2)
				when (top3 === 0.U || top3 === 7.U) {
					ap := ap << 1.U;
					an := an << 1.U;
					numerator := numerator << 1.U;
				} .elsewhen (4.U <= top3 && top3 <= 6.U) {
					ap := ap << 1.U
					an := (an << 1.U) | 1.U
					numerator := (numerator << 1.U) + (denominator << width.U)
				} .otherwise {
					ap := (ap << 1.U) | 1.U
					an := an << 1.U
					numerator := (numerator << 1.U) - (denominator << width.U)
				}

				i := i + 1.U
			}
		} .otherwise {
			io.in.ready := true.B
		}

		io.out.bits.quotient  := quotient
		io.out.bits.remainder := remainder
	}
}
