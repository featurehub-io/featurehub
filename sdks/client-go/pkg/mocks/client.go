// Code generated by counterfeiter. DO NOT EDIT.
package mocks

import (
	"sync"

	"github.com/featurehub-io/featurehub/sdks/client-go/pkg/interfaces"
	"github.com/featurehub-io/featurehub/sdks/client-go/pkg/models"
)

type FakeClient struct {
	AddNotifierBooleanStub        func(string, models.CallbackFuncBoolean)
	addNotifierBooleanMutex       sync.RWMutex
	addNotifierBooleanArgsForCall []struct {
		arg1 string
		arg2 models.CallbackFuncBoolean
	}
	AddNotifierFeatureStub        func(string, models.CallbackFuncFeature)
	addNotifierFeatureMutex       sync.RWMutex
	addNotifierFeatureArgsForCall []struct {
		arg1 string
		arg2 models.CallbackFuncFeature
	}
	AddNotifierJSONStub        func(string, models.CallbackFuncJSON)
	addNotifierJSONMutex       sync.RWMutex
	addNotifierJSONArgsForCall []struct {
		arg1 string
		arg2 models.CallbackFuncJSON
	}
	AddNotifierNumberStub        func(string, models.CallbackFuncNumber)
	addNotifierNumberMutex       sync.RWMutex
	addNotifierNumberArgsForCall []struct {
		arg1 string
		arg2 models.CallbackFuncNumber
	}
	AddNotifierStringStub        func(string, models.CallbackFuncString)
	addNotifierStringMutex       sync.RWMutex
	addNotifierStringArgsForCall []struct {
		arg1 string
		arg2 models.CallbackFuncString
	}
	DeleteNotifierStub        func(string) error
	deleteNotifierMutex       sync.RWMutex
	deleteNotifierArgsForCall []struct {
		arg1 string
	}
	deleteNotifierReturns struct {
		result1 error
	}
	deleteNotifierReturnsOnCall map[int]struct {
		result1 error
	}
	GetBooleanStub        func(string) (bool, error)
	getBooleanMutex       sync.RWMutex
	getBooleanArgsForCall []struct {
		arg1 string
	}
	getBooleanReturns struct {
		result1 bool
		result2 error
	}
	getBooleanReturnsOnCall map[int]struct {
		result1 bool
		result2 error
	}
	GetFeatureStub        func(string) (*models.FeatureState, error)
	getFeatureMutex       sync.RWMutex
	getFeatureArgsForCall []struct {
		arg1 string
	}
	getFeatureReturns struct {
		result1 *models.FeatureState
		result2 error
	}
	getFeatureReturnsOnCall map[int]struct {
		result1 *models.FeatureState
		result2 error
	}
	GetNumberStub        func(string) (float64, error)
	getNumberMutex       sync.RWMutex
	getNumberArgsForCall []struct {
		arg1 string
	}
	getNumberReturns struct {
		result1 float64
		result2 error
	}
	getNumberReturnsOnCall map[int]struct {
		result1 float64
		result2 error
	}
	GetRawJSONStub        func(string) (string, error)
	getRawJSONMutex       sync.RWMutex
	getRawJSONArgsForCall []struct {
		arg1 string
	}
	getRawJSONReturns struct {
		result1 string
		result2 error
	}
	getRawJSONReturnsOnCall map[int]struct {
		result1 string
		result2 error
	}
	GetStringStub        func(string) (string, error)
	getStringMutex       sync.RWMutex
	getStringArgsForCall []struct {
		arg1 string
	}
	getStringReturns struct {
		result1 string
		result2 error
	}
	getStringReturnsOnCall map[int]struct {
		result1 string
		result2 error
	}
	invocations      map[string][][]interface{}
	invocationsMutex sync.RWMutex
}

func (fake *FakeClient) AddNotifierBoolean(arg1 string, arg2 models.CallbackFuncBoolean) {
	fake.addNotifierBooleanMutex.Lock()
	fake.addNotifierBooleanArgsForCall = append(fake.addNotifierBooleanArgsForCall, struct {
		arg1 string
		arg2 models.CallbackFuncBoolean
	}{arg1, arg2})
	fake.recordInvocation("AddNotifierBoolean", []interface{}{arg1, arg2})
	fake.addNotifierBooleanMutex.Unlock()
	if fake.AddNotifierBooleanStub != nil {
		fake.AddNotifierBooleanStub(arg1, arg2)
	}
}

func (fake *FakeClient) AddNotifierBooleanCallCount() int {
	fake.addNotifierBooleanMutex.RLock()
	defer fake.addNotifierBooleanMutex.RUnlock()
	return len(fake.addNotifierBooleanArgsForCall)
}

func (fake *FakeClient) AddNotifierBooleanCalls(stub func(string, models.CallbackFuncBoolean)) {
	fake.addNotifierBooleanMutex.Lock()
	defer fake.addNotifierBooleanMutex.Unlock()
	fake.AddNotifierBooleanStub = stub
}

func (fake *FakeClient) AddNotifierBooleanArgsForCall(i int) (string, models.CallbackFuncBoolean) {
	fake.addNotifierBooleanMutex.RLock()
	defer fake.addNotifierBooleanMutex.RUnlock()
	argsForCall := fake.addNotifierBooleanArgsForCall[i]
	return argsForCall.arg1, argsForCall.arg2
}

func (fake *FakeClient) AddNotifierFeature(arg1 string, arg2 models.CallbackFuncFeature) {
	fake.addNotifierFeatureMutex.Lock()
	fake.addNotifierFeatureArgsForCall = append(fake.addNotifierFeatureArgsForCall, struct {
		arg1 string
		arg2 models.CallbackFuncFeature
	}{arg1, arg2})
	fake.recordInvocation("AddNotifierFeature", []interface{}{arg1, arg2})
	fake.addNotifierFeatureMutex.Unlock()
	if fake.AddNotifierFeatureStub != nil {
		fake.AddNotifierFeatureStub(arg1, arg2)
	}
}

func (fake *FakeClient) AddNotifierFeatureCallCount() int {
	fake.addNotifierFeatureMutex.RLock()
	defer fake.addNotifierFeatureMutex.RUnlock()
	return len(fake.addNotifierFeatureArgsForCall)
}

func (fake *FakeClient) AddNotifierFeatureCalls(stub func(string, models.CallbackFuncFeature)) {
	fake.addNotifierFeatureMutex.Lock()
	defer fake.addNotifierFeatureMutex.Unlock()
	fake.AddNotifierFeatureStub = stub
}

func (fake *FakeClient) AddNotifierFeatureArgsForCall(i int) (string, models.CallbackFuncFeature) {
	fake.addNotifierFeatureMutex.RLock()
	defer fake.addNotifierFeatureMutex.RUnlock()
	argsForCall := fake.addNotifierFeatureArgsForCall[i]
	return argsForCall.arg1, argsForCall.arg2
}

func (fake *FakeClient) AddNotifierJSON(arg1 string, arg2 models.CallbackFuncJSON) {
	fake.addNotifierJSONMutex.Lock()
	fake.addNotifierJSONArgsForCall = append(fake.addNotifierJSONArgsForCall, struct {
		arg1 string
		arg2 models.CallbackFuncJSON
	}{arg1, arg2})
	fake.recordInvocation("AddNotifierJSON", []interface{}{arg1, arg2})
	fake.addNotifierJSONMutex.Unlock()
	if fake.AddNotifierJSONStub != nil {
		fake.AddNotifierJSONStub(arg1, arg2)
	}
}

func (fake *FakeClient) AddNotifierJSONCallCount() int {
	fake.addNotifierJSONMutex.RLock()
	defer fake.addNotifierJSONMutex.RUnlock()
	return len(fake.addNotifierJSONArgsForCall)
}

func (fake *FakeClient) AddNotifierJSONCalls(stub func(string, models.CallbackFuncJSON)) {
	fake.addNotifierJSONMutex.Lock()
	defer fake.addNotifierJSONMutex.Unlock()
	fake.AddNotifierJSONStub = stub
}

func (fake *FakeClient) AddNotifierJSONArgsForCall(i int) (string, models.CallbackFuncJSON) {
	fake.addNotifierJSONMutex.RLock()
	defer fake.addNotifierJSONMutex.RUnlock()
	argsForCall := fake.addNotifierJSONArgsForCall[i]
	return argsForCall.arg1, argsForCall.arg2
}

func (fake *FakeClient) AddNotifierNumber(arg1 string, arg2 models.CallbackFuncNumber) {
	fake.addNotifierNumberMutex.Lock()
	fake.addNotifierNumberArgsForCall = append(fake.addNotifierNumberArgsForCall, struct {
		arg1 string
		arg2 models.CallbackFuncNumber
	}{arg1, arg2})
	fake.recordInvocation("AddNotifierNumber", []interface{}{arg1, arg2})
	fake.addNotifierNumberMutex.Unlock()
	if fake.AddNotifierNumberStub != nil {
		fake.AddNotifierNumberStub(arg1, arg2)
	}
}

func (fake *FakeClient) AddNotifierNumberCallCount() int {
	fake.addNotifierNumberMutex.RLock()
	defer fake.addNotifierNumberMutex.RUnlock()
	return len(fake.addNotifierNumberArgsForCall)
}

func (fake *FakeClient) AddNotifierNumberCalls(stub func(string, models.CallbackFuncNumber)) {
	fake.addNotifierNumberMutex.Lock()
	defer fake.addNotifierNumberMutex.Unlock()
	fake.AddNotifierNumberStub = stub
}

func (fake *FakeClient) AddNotifierNumberArgsForCall(i int) (string, models.CallbackFuncNumber) {
	fake.addNotifierNumberMutex.RLock()
	defer fake.addNotifierNumberMutex.RUnlock()
	argsForCall := fake.addNotifierNumberArgsForCall[i]
	return argsForCall.arg1, argsForCall.arg2
}

func (fake *FakeClient) AddNotifierString(arg1 string, arg2 models.CallbackFuncString) {
	fake.addNotifierStringMutex.Lock()
	fake.addNotifierStringArgsForCall = append(fake.addNotifierStringArgsForCall, struct {
		arg1 string
		arg2 models.CallbackFuncString
	}{arg1, arg2})
	fake.recordInvocation("AddNotifierString", []interface{}{arg1, arg2})
	fake.addNotifierStringMutex.Unlock()
	if fake.AddNotifierStringStub != nil {
		fake.AddNotifierStringStub(arg1, arg2)
	}
}

func (fake *FakeClient) AddNotifierStringCallCount() int {
	fake.addNotifierStringMutex.RLock()
	defer fake.addNotifierStringMutex.RUnlock()
	return len(fake.addNotifierStringArgsForCall)
}

func (fake *FakeClient) AddNotifierStringCalls(stub func(string, models.CallbackFuncString)) {
	fake.addNotifierStringMutex.Lock()
	defer fake.addNotifierStringMutex.Unlock()
	fake.AddNotifierStringStub = stub
}

func (fake *FakeClient) AddNotifierStringArgsForCall(i int) (string, models.CallbackFuncString) {
	fake.addNotifierStringMutex.RLock()
	defer fake.addNotifierStringMutex.RUnlock()
	argsForCall := fake.addNotifierStringArgsForCall[i]
	return argsForCall.arg1, argsForCall.arg2
}

func (fake *FakeClient) DeleteNotifier(arg1 string) error {
	fake.deleteNotifierMutex.Lock()
	ret, specificReturn := fake.deleteNotifierReturnsOnCall[len(fake.deleteNotifierArgsForCall)]
	fake.deleteNotifierArgsForCall = append(fake.deleteNotifierArgsForCall, struct {
		arg1 string
	}{arg1})
	fake.recordInvocation("DeleteNotifier", []interface{}{arg1})
	fake.deleteNotifierMutex.Unlock()
	if fake.DeleteNotifierStub != nil {
		return fake.DeleteNotifierStub(arg1)
	}
	if specificReturn {
		return ret.result1
	}
	fakeReturns := fake.deleteNotifierReturns
	return fakeReturns.result1
}

func (fake *FakeClient) DeleteNotifierCallCount() int {
	fake.deleteNotifierMutex.RLock()
	defer fake.deleteNotifierMutex.RUnlock()
	return len(fake.deleteNotifierArgsForCall)
}

func (fake *FakeClient) DeleteNotifierCalls(stub func(string) error) {
	fake.deleteNotifierMutex.Lock()
	defer fake.deleteNotifierMutex.Unlock()
	fake.DeleteNotifierStub = stub
}

func (fake *FakeClient) DeleteNotifierArgsForCall(i int) string {
	fake.deleteNotifierMutex.RLock()
	defer fake.deleteNotifierMutex.RUnlock()
	argsForCall := fake.deleteNotifierArgsForCall[i]
	return argsForCall.arg1
}

func (fake *FakeClient) DeleteNotifierReturns(result1 error) {
	fake.deleteNotifierMutex.Lock()
	defer fake.deleteNotifierMutex.Unlock()
	fake.DeleteNotifierStub = nil
	fake.deleteNotifierReturns = struct {
		result1 error
	}{result1}
}

func (fake *FakeClient) DeleteNotifierReturnsOnCall(i int, result1 error) {
	fake.deleteNotifierMutex.Lock()
	defer fake.deleteNotifierMutex.Unlock()
	fake.DeleteNotifierStub = nil
	if fake.deleteNotifierReturnsOnCall == nil {
		fake.deleteNotifierReturnsOnCall = make(map[int]struct {
			result1 error
		})
	}
	fake.deleteNotifierReturnsOnCall[i] = struct {
		result1 error
	}{result1}
}

func (fake *FakeClient) GetBoolean(arg1 string) (bool, error) {
	fake.getBooleanMutex.Lock()
	ret, specificReturn := fake.getBooleanReturnsOnCall[len(fake.getBooleanArgsForCall)]
	fake.getBooleanArgsForCall = append(fake.getBooleanArgsForCall, struct {
		arg1 string
	}{arg1})
	fake.recordInvocation("GetBoolean", []interface{}{arg1})
	fake.getBooleanMutex.Unlock()
	if fake.GetBooleanStub != nil {
		return fake.GetBooleanStub(arg1)
	}
	if specificReturn {
		return ret.result1, ret.result2
	}
	fakeReturns := fake.getBooleanReturns
	return fakeReturns.result1, fakeReturns.result2
}

func (fake *FakeClient) GetBooleanCallCount() int {
	fake.getBooleanMutex.RLock()
	defer fake.getBooleanMutex.RUnlock()
	return len(fake.getBooleanArgsForCall)
}

func (fake *FakeClient) GetBooleanCalls(stub func(string) (bool, error)) {
	fake.getBooleanMutex.Lock()
	defer fake.getBooleanMutex.Unlock()
	fake.GetBooleanStub = stub
}

func (fake *FakeClient) GetBooleanArgsForCall(i int) string {
	fake.getBooleanMutex.RLock()
	defer fake.getBooleanMutex.RUnlock()
	argsForCall := fake.getBooleanArgsForCall[i]
	return argsForCall.arg1
}

func (fake *FakeClient) GetBooleanReturns(result1 bool, result2 error) {
	fake.getBooleanMutex.Lock()
	defer fake.getBooleanMutex.Unlock()
	fake.GetBooleanStub = nil
	fake.getBooleanReturns = struct {
		result1 bool
		result2 error
	}{result1, result2}
}

func (fake *FakeClient) GetBooleanReturnsOnCall(i int, result1 bool, result2 error) {
	fake.getBooleanMutex.Lock()
	defer fake.getBooleanMutex.Unlock()
	fake.GetBooleanStub = nil
	if fake.getBooleanReturnsOnCall == nil {
		fake.getBooleanReturnsOnCall = make(map[int]struct {
			result1 bool
			result2 error
		})
	}
	fake.getBooleanReturnsOnCall[i] = struct {
		result1 bool
		result2 error
	}{result1, result2}
}

func (fake *FakeClient) GetFeature(arg1 string) (*models.FeatureState, error) {
	fake.getFeatureMutex.Lock()
	ret, specificReturn := fake.getFeatureReturnsOnCall[len(fake.getFeatureArgsForCall)]
	fake.getFeatureArgsForCall = append(fake.getFeatureArgsForCall, struct {
		arg1 string
	}{arg1})
	fake.recordInvocation("GetFeature", []interface{}{arg1})
	fake.getFeatureMutex.Unlock()
	if fake.GetFeatureStub != nil {
		return fake.GetFeatureStub(arg1)
	}
	if specificReturn {
		return ret.result1, ret.result2
	}
	fakeReturns := fake.getFeatureReturns
	return fakeReturns.result1, fakeReturns.result2
}

func (fake *FakeClient) GetFeatureCallCount() int {
	fake.getFeatureMutex.RLock()
	defer fake.getFeatureMutex.RUnlock()
	return len(fake.getFeatureArgsForCall)
}

func (fake *FakeClient) GetFeatureCalls(stub func(string) (*models.FeatureState, error)) {
	fake.getFeatureMutex.Lock()
	defer fake.getFeatureMutex.Unlock()
	fake.GetFeatureStub = stub
}

func (fake *FakeClient) GetFeatureArgsForCall(i int) string {
	fake.getFeatureMutex.RLock()
	defer fake.getFeatureMutex.RUnlock()
	argsForCall := fake.getFeatureArgsForCall[i]
	return argsForCall.arg1
}

func (fake *FakeClient) GetFeatureReturns(result1 *models.FeatureState, result2 error) {
	fake.getFeatureMutex.Lock()
	defer fake.getFeatureMutex.Unlock()
	fake.GetFeatureStub = nil
	fake.getFeatureReturns = struct {
		result1 *models.FeatureState
		result2 error
	}{result1, result2}
}

func (fake *FakeClient) GetFeatureReturnsOnCall(i int, result1 *models.FeatureState, result2 error) {
	fake.getFeatureMutex.Lock()
	defer fake.getFeatureMutex.Unlock()
	fake.GetFeatureStub = nil
	if fake.getFeatureReturnsOnCall == nil {
		fake.getFeatureReturnsOnCall = make(map[int]struct {
			result1 *models.FeatureState
			result2 error
		})
	}
	fake.getFeatureReturnsOnCall[i] = struct {
		result1 *models.FeatureState
		result2 error
	}{result1, result2}
}

func (fake *FakeClient) GetNumber(arg1 string) (float64, error) {
	fake.getNumberMutex.Lock()
	ret, specificReturn := fake.getNumberReturnsOnCall[len(fake.getNumberArgsForCall)]
	fake.getNumberArgsForCall = append(fake.getNumberArgsForCall, struct {
		arg1 string
	}{arg1})
	fake.recordInvocation("GetNumber", []interface{}{arg1})
	fake.getNumberMutex.Unlock()
	if fake.GetNumberStub != nil {
		return fake.GetNumberStub(arg1)
	}
	if specificReturn {
		return ret.result1, ret.result2
	}
	fakeReturns := fake.getNumberReturns
	return fakeReturns.result1, fakeReturns.result2
}

func (fake *FakeClient) GetNumberCallCount() int {
	fake.getNumberMutex.RLock()
	defer fake.getNumberMutex.RUnlock()
	return len(fake.getNumberArgsForCall)
}

func (fake *FakeClient) GetNumberCalls(stub func(string) (float64, error)) {
	fake.getNumberMutex.Lock()
	defer fake.getNumberMutex.Unlock()
	fake.GetNumberStub = stub
}

func (fake *FakeClient) GetNumberArgsForCall(i int) string {
	fake.getNumberMutex.RLock()
	defer fake.getNumberMutex.RUnlock()
	argsForCall := fake.getNumberArgsForCall[i]
	return argsForCall.arg1
}

func (fake *FakeClient) GetNumberReturns(result1 float64, result2 error) {
	fake.getNumberMutex.Lock()
	defer fake.getNumberMutex.Unlock()
	fake.GetNumberStub = nil
	fake.getNumberReturns = struct {
		result1 float64
		result2 error
	}{result1, result2}
}

func (fake *FakeClient) GetNumberReturnsOnCall(i int, result1 float64, result2 error) {
	fake.getNumberMutex.Lock()
	defer fake.getNumberMutex.Unlock()
	fake.GetNumberStub = nil
	if fake.getNumberReturnsOnCall == nil {
		fake.getNumberReturnsOnCall = make(map[int]struct {
			result1 float64
			result2 error
		})
	}
	fake.getNumberReturnsOnCall[i] = struct {
		result1 float64
		result2 error
	}{result1, result2}
}

func (fake *FakeClient) GetRawJSON(arg1 string) (string, error) {
	fake.getRawJSONMutex.Lock()
	ret, specificReturn := fake.getRawJSONReturnsOnCall[len(fake.getRawJSONArgsForCall)]
	fake.getRawJSONArgsForCall = append(fake.getRawJSONArgsForCall, struct {
		arg1 string
	}{arg1})
	fake.recordInvocation("GetRawJSON", []interface{}{arg1})
	fake.getRawJSONMutex.Unlock()
	if fake.GetRawJSONStub != nil {
		return fake.GetRawJSONStub(arg1)
	}
	if specificReturn {
		return ret.result1, ret.result2
	}
	fakeReturns := fake.getRawJSONReturns
	return fakeReturns.result1, fakeReturns.result2
}

func (fake *FakeClient) GetRawJSONCallCount() int {
	fake.getRawJSONMutex.RLock()
	defer fake.getRawJSONMutex.RUnlock()
	return len(fake.getRawJSONArgsForCall)
}

func (fake *FakeClient) GetRawJSONCalls(stub func(string) (string, error)) {
	fake.getRawJSONMutex.Lock()
	defer fake.getRawJSONMutex.Unlock()
	fake.GetRawJSONStub = stub
}

func (fake *FakeClient) GetRawJSONArgsForCall(i int) string {
	fake.getRawJSONMutex.RLock()
	defer fake.getRawJSONMutex.RUnlock()
	argsForCall := fake.getRawJSONArgsForCall[i]
	return argsForCall.arg1
}

func (fake *FakeClient) GetRawJSONReturns(result1 string, result2 error) {
	fake.getRawJSONMutex.Lock()
	defer fake.getRawJSONMutex.Unlock()
	fake.GetRawJSONStub = nil
	fake.getRawJSONReturns = struct {
		result1 string
		result2 error
	}{result1, result2}
}

func (fake *FakeClient) GetRawJSONReturnsOnCall(i int, result1 string, result2 error) {
	fake.getRawJSONMutex.Lock()
	defer fake.getRawJSONMutex.Unlock()
	fake.GetRawJSONStub = nil
	if fake.getRawJSONReturnsOnCall == nil {
		fake.getRawJSONReturnsOnCall = make(map[int]struct {
			result1 string
			result2 error
		})
	}
	fake.getRawJSONReturnsOnCall[i] = struct {
		result1 string
		result2 error
	}{result1, result2}
}

func (fake *FakeClient) GetString(arg1 string) (string, error) {
	fake.getStringMutex.Lock()
	ret, specificReturn := fake.getStringReturnsOnCall[len(fake.getStringArgsForCall)]
	fake.getStringArgsForCall = append(fake.getStringArgsForCall, struct {
		arg1 string
	}{arg1})
	fake.recordInvocation("GetString", []interface{}{arg1})
	fake.getStringMutex.Unlock()
	if fake.GetStringStub != nil {
		return fake.GetStringStub(arg1)
	}
	if specificReturn {
		return ret.result1, ret.result2
	}
	fakeReturns := fake.getStringReturns
	return fakeReturns.result1, fakeReturns.result2
}

func (fake *FakeClient) GetStringCallCount() int {
	fake.getStringMutex.RLock()
	defer fake.getStringMutex.RUnlock()
	return len(fake.getStringArgsForCall)
}

func (fake *FakeClient) GetStringCalls(stub func(string) (string, error)) {
	fake.getStringMutex.Lock()
	defer fake.getStringMutex.Unlock()
	fake.GetStringStub = stub
}

func (fake *FakeClient) GetStringArgsForCall(i int) string {
	fake.getStringMutex.RLock()
	defer fake.getStringMutex.RUnlock()
	argsForCall := fake.getStringArgsForCall[i]
	return argsForCall.arg1
}

func (fake *FakeClient) GetStringReturns(result1 string, result2 error) {
	fake.getStringMutex.Lock()
	defer fake.getStringMutex.Unlock()
	fake.GetStringStub = nil
	fake.getStringReturns = struct {
		result1 string
		result2 error
	}{result1, result2}
}

func (fake *FakeClient) GetStringReturnsOnCall(i int, result1 string, result2 error) {
	fake.getStringMutex.Lock()
	defer fake.getStringMutex.Unlock()
	fake.GetStringStub = nil
	if fake.getStringReturnsOnCall == nil {
		fake.getStringReturnsOnCall = make(map[int]struct {
			result1 string
			result2 error
		})
	}
	fake.getStringReturnsOnCall[i] = struct {
		result1 string
		result2 error
	}{result1, result2}
}

func (fake *FakeClient) Invocations() map[string][][]interface{} {
	fake.invocationsMutex.RLock()
	defer fake.invocationsMutex.RUnlock()
	fake.addNotifierBooleanMutex.RLock()
	defer fake.addNotifierBooleanMutex.RUnlock()
	fake.addNotifierFeatureMutex.RLock()
	defer fake.addNotifierFeatureMutex.RUnlock()
	fake.addNotifierJSONMutex.RLock()
	defer fake.addNotifierJSONMutex.RUnlock()
	fake.addNotifierNumberMutex.RLock()
	defer fake.addNotifierNumberMutex.RUnlock()
	fake.addNotifierStringMutex.RLock()
	defer fake.addNotifierStringMutex.RUnlock()
	fake.deleteNotifierMutex.RLock()
	defer fake.deleteNotifierMutex.RUnlock()
	fake.getBooleanMutex.RLock()
	defer fake.getBooleanMutex.RUnlock()
	fake.getFeatureMutex.RLock()
	defer fake.getFeatureMutex.RUnlock()
	fake.getNumberMutex.RLock()
	defer fake.getNumberMutex.RUnlock()
	fake.getRawJSONMutex.RLock()
	defer fake.getRawJSONMutex.RUnlock()
	fake.getStringMutex.RLock()
	defer fake.getStringMutex.RUnlock()
	copiedInvocations := map[string][][]interface{}{}
	for key, value := range fake.invocations {
		copiedInvocations[key] = value
	}
	return copiedInvocations
}

func (fake *FakeClient) recordInvocation(key string, args []interface{}) {
	fake.invocationsMutex.Lock()
	defer fake.invocationsMutex.Unlock()
	if fake.invocations == nil {
		fake.invocations = map[string][][]interface{}{}
	}
	if fake.invocations[key] == nil {
		fake.invocations[key] = [][]interface{}{}
	}
	fake.invocations[key] = append(fake.invocations[key], args)
}

var _ interfaces.Client = new(FakeClient)
