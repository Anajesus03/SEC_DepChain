// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";
import "BlackList.sol";

contract ISTCoin is ERC20, Blacklist {
    constructor() ERC20("IST Coin", "IST") {
        _mint(msg.sender, 100000000 * 10**2); // 100 million tokens with 2 decimals
    }

    function _update(address from, address to, uint256 value) internal override {
        require(!isBlacklisted(from), "Sender is blacklisted");
        require(!isBlacklisted(to), "Receiver is blacklisted");
        super._update(from, to, value); // Call the parent function
    }
}