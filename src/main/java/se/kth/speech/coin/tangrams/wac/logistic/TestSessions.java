/*
 * 	Copyright 2018 Todd Shore
 *
 *	Licensed under the Apache License, Version 2.0 (the "License");
 *	you may not use this file except in compliance with the License.
 *	You may obtain a copy of the License at
 *
 *		http://www.apache.org/licenses/LICENSE-2.0
 *
 *	Unless required by applicable law or agreed to in writing, software
 *	distributed under the License is distributed on an "AS IS" BASIS,
 *	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *	See the License for the specific language governing permissions and
 *	limitations under the License.
 */
package se.kth.speech.coin.tangrams.wac.logistic;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import se.kth.speech.coin.tangrams.wac.data.Session;

/**
 * @author <a href="mailto:errantlinguist+github@gmail.com">Todd Shore</a>
 * @since 13 Feb 2018
 *
 */
final class TestSessions {
	
	private static final Set<String> TEST_SESSION_NAMES = IntStream.of(22, 40, 6, 21, 12, 17, 35, 10, 5, 38).mapToObj(Integer::toString).collect(Collectors.toSet());
	
	public static boolean isTestSession(Session session) {
		return TEST_SESSION_NAMES.contains(session.getName());
	}
	
	private TestSessions(){
		
	}

}
