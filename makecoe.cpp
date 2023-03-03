#include <array>
#include <cerrno>
#include <cstring>
#include <iomanip>
#include <iostream>
#include <string>
#include <vector>

int main(int argc, char **argv) {
	constexpr size_t BUFFER_SIZE = 1024;
	size_t grouping = 2; // in nybbles

	if (1 < argc)
		grouping = std::strtoul(argv[1], nullptr, 10);

	if (grouping == 0) {
		std::cerr << "Invalid grouping.\n";
		return 1;
	}

	std::freopen(nullptr, "rb", stdin);

	if (std::ferror(stdin))
		throw std::runtime_error(std::strerror(errno));

	std::size_t len;
	std::array<uint8_t, BUFFER_SIZE> buf;
	std::vector<uint8_t> input;

	while (0 < (len = std::fread(buf.data(), sizeof(buf[0]), buf.size(), stdin))) {
		if (std::ferror(stdin) && !std::feof(stdin))
			throw std::runtime_error(std::strerror(errno));
		input.insert(input.end(), buf.data(), buf.data() + len);
	}

	std::cout << "memory_initialization_radix=16;\n";
	std::cout << "memory_initialization_vector=";
	std::cout << std::hex;
	for (size_t i = 0, max = input.size(); i < 2 * max; i += grouping) {
		std::cout << '\n';
		for (size_t j = 0; j < grouping; ++j)
			std::cout << std::setw(1) << std::setfill('0') << static_cast<uint32_t>(max <= (i + j) / 2? 0 : ((input[(i + j) / 2] >> ((i + j) % 2? 0 : 4) & 0xf)));
		std::cout << (2 * max <= i + grouping? ';' : ',');
	}
	std::cout << std::endl;
}