## FPGABoy

FPGABoy is a Chisel3 implementation of the Game Boy and NES audio processing units (APUs) as a final project for CSE 228A at UCSC. Both chips are complete, except for the NES DMC channel, which can't be implemented due to technical constraints. Audio is output over HDMI and aux on the Nexys Video board. For style points, this project implements video rendering as well. It displays the slides for the presentation (use the left/right buttons).

### Testing

Tests are currently incomplete. The only working test that does any useful verification is `DividerTests`. `GameBoyTests` and `NESTests` run for a *very* long time (multiple hours) and produce VCD outputs for manual inspection. Run `make testDivider` to run `DividerTests`, `make testGB` to run `GameBoyTests` or `make testNES` to run `NESTests`.

### Building

TODO. I'll probably include a zip of the Vivado project, but it will likely include absolute paths that will have to be fixed manually. I'll also include instructions on how to set up the project from scratch.

### Bitstreams

Sample Nexys Video bitstreams from various stages in the project are available in the `bits` directory. `trippy3.bit` and `demos1.bit` are good demos (press the center button to start the music), while `loudnoise.bit` is *definitely* not.