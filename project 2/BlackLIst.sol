// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";
import "@openzeppelin/contracts/access/Ownable.sol";

contract Blacklist is Ownable {
    constructor() Ownable(msg.sender) {}  // Pass deployer as initial owner

    mapping(address => bool) private blacklisted;


    event AddedToBlacklist(address indexed account);
    event RemovedFromBlacklist(address indexed account);

    function addToBlacklist(address account) public returns (string memory) {
        if (blacklisted[account] ) {return "Account already blacklisted";}
        blacklisted[account] = true;
        emit AddedToBlacklist(account);
        return "added";
    }

    function removeFromBlacklist(address account) public returns (bool) {
        require(blacklisted[account], "Account is not blacklisted");
        blacklisted[account] = false;
        emit RemovedFromBlacklist(account);
        return true;
    }

    function isBlacklisted(address account) public view returns (bool) {
        return blacklisted[account];
    }

    function isBlacklisted_String(address account) public view returns (string memory) {
        if(blacklisted[account]){return "True, it is blacklisted";}
        return "False, it is not blacklisted";
    }
    function sayHelloWorld() public pure returns (string memory){
        return "hello";
    }
}


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

